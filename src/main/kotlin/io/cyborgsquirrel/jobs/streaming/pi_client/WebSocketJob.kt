package io.cyborgsquirrel.jobs.streaming.pi_client

import io.cyborgsquirrel.clients.config.pi_client.PiConfigClient
import io.cyborgsquirrel.clients.config.pi_client.PiClientConfig
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.lighting.effect_trigger.service.TriggerManager
import io.cyborgsquirrel.jobs.streaming.ClientStreamingJob
import io.cyborgsquirrel.jobs.streaming.StreamingJobState
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.RgbFrameData
import io.cyborgsquirrel.lighting.model.RgbFrameOptionsBuilder
import io.cyborgsquirrel.lighting.rendering.LightEffectRenderer
import io.cyborgsquirrel.jobs.streaming.serialization.PiFrameDataSerializer
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.lighting.power_limits.PowerLimiterService
import io.cyborgsquirrel.util.time.TimeHelper
import io.micronaut.http.uri.UriBuilder
import io.micronaut.websocket.WebSocketClient
import kotlinx.coroutines.*
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.slf4j.LoggerFactory
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Background job for streaming light effects to Raspberry Pi clients
 *
 * Communicates with the Raspberry Pi client with the following steps
 * 1. Checks if the Pi client and LED strips are configured.
 * If at any point one or both of these is deleted the job returns to this step.
 * 2. A WebSocket connection is established.
 * 3. Syncs settings if a settings sync hasn't been performed. This step ensures the Pi client has the same configuration
 * as the Color Composer server.
 * 4. Syncs time with the Pi client. This step may be done again if during the rendering process a time de-sync is detected.
 * 5. Effect rendering. The light effect RGB data is streamed to the Pi client's WebSocket. If the buffer is full the
 * job goes to the [StreamingJobState.BufferFullWaiting] state to wait for the Pi to render one or more frames.
 */
class WebSocketJob(
    private val webSocketClient: WebSocketClient,
    private val renderer: LightEffectRenderer,
    private val triggerManager: TriggerManager,
    private val clientRepository: H2LedStripClientRepository,
    private val timeHelper: TimeHelper,
    private val piConfigClient: PiConfigClient,
    private var clientEntity: LedStripClientEntity,
) : ClientStreamingJob {

    // Pi WebSocket client
    private var client: LedStripWebSocketClient? = null

    // Client data
    private lateinit var strip: LedStripEntity

    // Serialization
    private val serializer = PiFrameDataSerializer()

    // Time tracking
    private val timeDesyncToleranceMillis = 5
    private var lastTimeSyncPerformedAt = 0L
    private val timeSinceLastSync: Long
        get() = timeHelper.millisSinceEpoch() - lastTimeSyncPerformedAt
    private var lastKeepaliveFrameTimestamp = LocalDateTime.of(0, 1, 1, 0, 0)
    private var timestampMillis = 0L
    private var sleepMillis = 0L

    // Difference in millis between the client and server.
    // Negative values mean the client's clock is behind the server, positive values mean the client's clock is ahead.
    private var clientTimeOffset: Long = 0L

    // State/logic
    private var exponentialReconnectionBackoffValue = 1
    private val exponentialReconnectionBackoffValueMax = 8
    private val fps = 35
    private val bufferTimeInMilliseconds = 500L
    private var shouldRun = true
    private var settingsSyncRequired = true
    private var state = StreamingJobState.SetupIncomplete

    /**
     * Starts the job which will run in the background using a Kotlin Coroutine.
     * Returns the Job instance.
     */
    override fun start(scope: CoroutineScope): Job {
        return scope.launch {
            logger.info("Start")
            while (isActive && shouldRun) {
                processState()
            }
            logger.info("Done")
        }
    }

    override fun getCurrentState() = state

    private suspend fun processState() {
        try {
            when (state) {
                StreamingJobState.SetupIncomplete -> {
                    val clientOptional = clientRepository.findByUuid(clientEntity.uuid!!)
                    if (clientOptional.isPresent) {
                        clientEntity = clientOptional.get()
                        if (clientEntity.strips.isNotEmpty()) {
                            strip = clientEntity.strips.first()
                            timestampMillis = timeHelper.millisSinceEpoch() + (1000 / fps) + clientTimeOffset
                            state = StreamingJobState.WaitingForConnection
                            setupSocket()
                        } else {
                            delay(5000)
                        }
                    } else {
                        delay(5000)
                    }
                }

                StreamingJobState.WaitingForConnection -> {
                    delay(50)
                }

                StreamingJobState.SettingsSync -> {
                    logger.info("Syncing settings with $clientEntity")
                    settingsSyncRequired = false
                    val clientConfig = piConfigClient.getConfigs(clientEntity)
                    val serverConfig = PiClientConfig(
                        strip.uuid!!,
                        strip.pin!!,
                        strip.length!!,
                        strip.brightness!!,
                        clientEntity.colorOrder!!
                    )
                    val matching = clientConfig.configList.size == 1 && clientConfig.configList.first() == serverConfig

                    if (!matching) {
                        logger.info("Settings out of sync for $clientEntity - server config: $serverConfig")

                        for (config in clientConfig.configList) {
                            piConfigClient.deleteConfig(clientEntity, config)
                        }
                        piConfigClient.addConfig(clientEntity, serverConfig)
                    }

                    state = StreamingJobState.ConnectedIdle
                }

                StreamingJobState.ConnectedIdle -> {
                    exponentialReconnectionBackoffValue = 1
                    state = if (settingsSyncRequired) {
                        StreamingJobState.SettingsSync
                    } else if (lastTimeSyncPerformedAt == 0L) {
                        StreamingJobState.TimeSyncRequired
                    } else {
                        StreamingJobState.RenderingEffect
                    }
                }

                StreamingJobState.DisconnectedIdle -> {
                    logger.info("Client $clientEntity disconnected. Attempting to reconnect...")
                    settingsSyncRequired = true
                    state = StreamingJobState.WaitingForConnection
                    setupSocket()
                }

                StreamingJobState.BufferFullWaiting -> {
                    delay(sleepMillis)
                    state = StreamingJobState.RenderingEffect
                }

                StreamingJobState.TimeSyncRequired -> {
                    syncClientTime()
                    timestampMillis = timeHelper.millisSinceEpoch() + clientTimeOffset
                    logger.info("New timestamp ${timeHelper.dateTimeFromMillis(timestampMillis)} millis $timestampMillis")

                    // If we disconnect during the time sync don't set the state to connected
                    if (state != StreamingJobState.DisconnectedIdle) {
                        state = StreamingJobState.RenderingEffect
                    }
                }

                StreamingJobState.RenderingEffect -> {
                    val currentTimeAsMillis = timeHelper.millisSinceEpoch()
                    val timeDesynced =
                        timestampMillis + timeDesyncToleranceMillis < currentTimeAsMillis + clientTimeOffset
                    if (timeDesynced) {
                        logger.info(
                            "Re-syncing time with client $client - (client time offset ${clientTimeOffset}ms frame timestamp: ${
                                timeHelper.dateTimeFromMillis(
                                    timestampMillis
                                )
                            })"
                        )
                        state = StreamingJobState.TimeSyncRequired
                    } else {
                        triggerManager.processTriggers()
                        val frameOptional = renderer.renderFrame(strip.uuid!!, 0)
                        if (frameOptional.isEmpty) {
                            // Send a keep-alive frame to clear the strip and prevent the WebSocket from timing out
                            if (lastKeepaliveFrameTimestamp.plusMinutes(1).isBefore(LocalDateTime.now())) {
                                logger.info("Sending keep-alive frame to $clientEntity")
                                sendClearFrame()
                                lastKeepaliveFrameTimestamp = LocalDateTime.now()
                            } else {
                                delay(250)
                            }

                            timestampMillis = timeHelper.millisSinceEpoch() + clientTimeOffset
                        } else {
                            // Time tracking - reset last keep-alive timestamp to ensure if effects are stopped the empty
                            // frame logic will immediately send sendClearFrame() to clear the strip.
                            lastKeepaliveFrameTimestamp = LocalDateTime.of(0, 1, 1, 0, 0)
                            timestampMillis += 1000 / fps

                            // Assemble RGB data - timestamp of 0L to not buffer the frame if all effects are paused
                            val frame = frameOptional.get()
                            val rgbData = frame.frameData
                            val frameData = RgbFrameData(timestampMillis, rgbData)

                            // Build options - clear the frame buffer if all effects are paused, this makes the LED strip more responsive to play/pause commands
                            val optionsBuilder = RgbFrameOptionsBuilder()
                            val options = optionsBuilder.build()

                            // Serialize and send frame
                            val encodedFrame = serializer.encode(frameData, strip.pin!!, options)

                            withContext(Dispatchers.IO) {
                                client?.send(encodedFrame)?.get(1, TimeUnit.SECONDS)
                            }

                            // Slow down to ensure we only buffer the specified amount of time into the future.
                            val nowPlusBufferMillis =
                                Timestamp.from(Instant.now().plusMillis(bufferTimeInMilliseconds)).time
                            if (nowPlusBufferMillis < timestampMillis) {
                                sleepMillis = timestampMillis - nowPlusBufferMillis
                                state = StreamingJobState.BufferFullWaiting
                            } else if (frame.allEffectsPaused) {
                                // Wait for the duration of one frame to reduce time spent rendering/sending frames
                                // if all effects are paused.
                                sleepMillis = (1000 / fps).toLong()
                                timestampMillis += sleepMillis
                                state = StreamingJobState.BufferFullWaiting
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            logger.error("Error $ex while processing state $state")
            ex.printStackTrace()
            delay((2 shl exponentialReconnectionBackoffValue) * 1000L)
            if (exponentialReconnectionBackoffValue < exponentialReconnectionBackoffValueMax) exponentialReconnectionBackoffValue++
        }
    }

    private suspend fun sendClearFrame() {
        val rgbData = mutableListOf<RgbColor>()
        for (i in 0..<strip.length!!) {
            rgbData.add(RgbColor.Blank)
        }

        val frameData = RgbFrameData(0, rgbData)
        val optionsBuilder = RgbFrameOptionsBuilder()
        optionsBuilder.setClearBuffer()
        val options = optionsBuilder.build()
        val frame = serializer.encode(frameData, strip.pin!!, options)

        withContext(Dispatchers.IO) {
            client?.send(frame)?.get(1, TimeUnit.SECONDS)
        }
    }

    private suspend fun syncClientTime() {
        // Don't sync more often than every 3 seconds if we end up looping on time sync for some reason
        if (timeSinceLastSync > 1000 * 3) {
            val requestTimestamp = timeHelper.millisSinceEpoch()
            val clientTime = piConfigClient.getClientTime(clientEntity)
            val responseTimestamp = timeHelper.millisSinceEpoch()

            // Assume request and response take the same amount of time for the network to transmit
            // Divide by 2 so we only count the transmission time going one way
            val requestResponseDuration = (responseTimestamp - requestTimestamp) / 2
            val adjustedClientTime = clientTime.millisSinceEpoch - requestResponseDuration
            clientTimeOffset = adjustedClientTime - responseTimestamp

            lastTimeSyncPerformedAt = responseTimestamp
            logger.info("Client time sync complete. Offset in millis: $clientTimeOffset")
        }
    }

    private suspend fun setupSocket() {
        val httpPattern = Regex("^(http|https)")
        val httpPatternResult = httpPattern.find(clientEntity.address!!)
        val websocketAddress = if (httpPatternResult?.groups?.isNotEmpty() == true) {
            clientEntity.address!!.replace(httpPattern, "ws")
        } else {
            "ws://${clientEntity.address!!}"
        }
        val uri = UriBuilder.of(websocketAddress).port(clientEntity.wsPort!!).build()
        val future = CompletableFuture<LedStripWebSocketClient>()
        val clientPublisher = webSocketClient.connect(LedStripWebSocketClient::class.java, uri)
        clientPublisher.subscribe(object : Subscriber<LedStripWebSocketClient> {
            override fun onSubscribe(s: Subscription?) {
                s?.request(1)
            }

            override fun onError(t: Throwable?) {
                state = StreamingJobState.DisconnectedIdle
                future.completeExceptionally(t)
            }

            override fun onComplete() {}

            override fun onNext(client: LedStripWebSocketClient?) {
                state = StreamingJobState.ConnectedIdle
                client?.registerOnDisconnectedCallback({
                    if (state != StreamingJobState.SetupIncomplete) {
                        state = StreamingJobState.DisconnectedIdle
                    }
                })
                future.complete(client)
            }
        })

        client = withContext(Dispatchers.IO) {
            future.get(5, TimeUnit.SECONDS)
        }
    }

    override fun dispose() {
        shouldRun = false
        client?.unregisterOnDisconnectedCallback()
        client?.close()
    }

    override fun onDataUpdate() {
        settingsSyncRequired = true
        client?.close()
        state = StreamingJobState.SetupIncomplete
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebSocketJob::class.java)
    }
}