package io.cyborgsquirrel.lighting.job

import io.cyborgsquirrel.clients.config.ConfigClient
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.lighting.client.LedStripWebSocketClient
import io.cyborgsquirrel.lighting.effect_trigger.service.TriggerManager
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.RgbFrameData
import io.cyborgsquirrel.lighting.model.RgbFrameOptionsBuilder
import io.cyborgsquirrel.lighting.rendering.LightEffectRenderer
import io.cyborgsquirrel.lighting.serialization.RgbFrameDataSerializer
import io.cyborgsquirrel.util.time.TimeHelper
import io.micronaut.http.uri.UriBuilder
import io.micronaut.websocket.WebSocketClient
import kotlinx.coroutines.*
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.slf4j.LoggerFactory
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Background job for streaming effects to clients
 */
class WebSocketJob(
    private val webSocketClient: WebSocketClient,
    private val renderer: LightEffectRenderer,
    private val triggerManager: TriggerManager,
    private val clientRepository: H2LedStripClientRepository,
    private val timeHelper: TimeHelper,
    private val configClient: ConfigClient,
    private var clientEntity: LedStripClientEntity,
) : DisposableHandle {

    private val serializer = RgbFrameDataSerializer()
    private var client: LedStripWebSocketClient? = null
    private lateinit var strip: LedStripModel

    // Difference in millis between the client and server.
    // Negative values mean the client's clock is behind the server, positive values mean the client's clock is ahead.
    private var clientTimeOffset: Long = 0L
    private val fps = 35
    private val bufferTimeInSeconds = 1L
    private val timeDesyncToleranceMillis = 5
    private var lastTimeSyncPerformedAt = 0L
    private val timeSinceLastSync: Long
        get() = timeHelper.millisSinceEpoch() - lastTimeSyncPerformedAt
    private var state = WebSocketState.InsufficientData
    private var shouldRun = true
    private var lastKeepaliveFrameTimestamp = LocalDateTime.of(0, 1, 1, 0, 0)
    private var timestampMillis = 0L
    private var sleepMillis = 0L
    private var exponentialReconnectionBackoffValue = 1

    fun start(scope: CoroutineScope): Job {
        return scope.launch {
            logger.info("Start")
            while (isActive && shouldRun) {
                processState()
            }
            logger.info("Done")
        }
    }

    private suspend fun processState() {
        try {
            when (state) {
                WebSocketState.InsufficientData -> {
                    val clientOptional = clientRepository.findByUuid(clientEntity.uuid!!)
                    if (clientOptional.isPresent) {
                        clientEntity = clientOptional.get()
                        if (clientEntity.strips.isNotEmpty()) {
                            val stripEntity = clientEntity.strips.first()
                            strip = LedStripModel(
                                stripEntity.name!!,
                                stripEntity.uuid!!,
                                stripEntity.length!!,
                                stripEntity.height,
                                stripEntity.blendMode!!
                            )
                            timestampMillis = timeHelper.millisSinceEpoch() + (1000 / fps) + clientTimeOffset
                            state = WebSocketState.WaitingForConnection
                            setupWebSocket()
                        } else {
                            delay(5000)
                        }
                    } else {
                        delay(5000)
                    }
                }

                WebSocketState.WaitingForConnection -> {
                    delay(50)
                }

                WebSocketState.ConnectedIdle -> {
                    exponentialReconnectionBackoffValue = 1
                    state = if (lastTimeSyncPerformedAt == 0L) {
                        WebSocketState.TimeSyncRequired
                    } else {
                        WebSocketState.RenderingEffect
                    }
                }

                WebSocketState.DisconnectedIdle -> {
                    try {
                        logger.info("Client disconnected. Attempting to reconnect...")
                        state = WebSocketState.WaitingForConnection
                        setupWebSocket()
                    } catch (ex: Exception) {
                        logger.info("Unable to connect to client $clientEntity")
                        if (exponentialReconnectionBackoffValue < 7) exponentialReconnectionBackoffValue++
                        delay((2 shl exponentialReconnectionBackoffValue) * 1000L)
                    }
                }

                WebSocketState.BufferFullWaiting -> {
                    delay(sleepMillis)
                    state = WebSocketState.RenderingEffect
                }

                WebSocketState.TimeSyncRequired -> {
                    syncClientTime()
                    timestampMillis = timeHelper.millisSinceEpoch() + clientTimeOffset
                    logger.info("New timestamp ${timeHelper.dateTimeFromMillis(timestampMillis)}")

                    // If we disconnect during the time sync don't set the state to connected
                    if (state != WebSocketState.DisconnectedIdle) {
                        state = WebSocketState.RenderingEffect
                    }
                }

                WebSocketState.RenderingEffect -> {
                    val currentTimeAsMillis = timeHelper.millisSinceEpoch()
                    val timeDesynced =
                        timestampMillis + timeDesyncToleranceMillis < currentTimeAsMillis + clientTimeOffset
                    val lastTimeSyncMoreThan5MinsAgo =
                        currentTimeAsMillis - lastTimeSyncPerformedAt > Duration.ofMinutes(5).toMillis()
                    if (timeDesynced || lastTimeSyncMoreThan5MinsAgo) {
                        logger.info(
                            "Re-syncing time with client $client - (client time offset ${clientTimeOffset}ms frame timestamp: ${
                                timeHelper.dateTimeFromMillis(
                                    timestampMillis
                                )
                            })"
                        )
                        state = WebSocketState.TimeSyncRequired
                    } else {
                        triggerManager.processTriggers()
                        val frame = renderer.renderFrame(strip.getUuid(), 0)
                        if (frame.isEmpty) {
                            if (lastKeepaliveFrameTimestamp.plusMinutes(1).isBefore(LocalDateTime.now())) {
                                logger.info("Sending keep-alive frame")
                                sendClearFrame()
                                lastKeepaliveFrameTimestamp = LocalDateTime.now()
                            } else {
                                delay(250)
                            }

                            timestampMillis = timeHelper.millisSinceEpoch() + clientTimeOffset
                        } else {
                            lastKeepaliveFrameTimestamp = LocalDateTime.of(0, 1, 1, 0, 0)
                            timestampMillis += 1000 / fps
                            val rgbData = frame.get().frameData
                            val frameData = RgbFrameData(timestampMillis, rgbData)
                            val encodedFrame = serializer.encode(frameData)
                            withContext(Dispatchers.IO) {
                                client?.send(encodedFrame)?.get(1, TimeUnit.SECONDS)
                            }

                            // Slow down to ensure we only buffer the specified amount of time into the future.
                            val nowPlusBufferSeconds =
                                Timestamp.from(Instant.now().plusSeconds(bufferTimeInSeconds)).time
                            if (nowPlusBufferSeconds < timestampMillis) {
                                sleepMillis = timestampMillis - nowPlusBufferSeconds
                                state = WebSocketState.BufferFullWaiting
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            logger.error("Error $ex while processing state $state")
            ex.printStackTrace()
            delay((2 shl exponentialReconnectionBackoffValue) * 1000L)
            if (exponentialReconnectionBackoffValue < 7) exponentialReconnectionBackoffValue++
        }
    }

    private suspend fun sendClearFrame() {
        val rgbData = mutableListOf<RgbColor>()
        for (i in 0..<strip.getLength()) {
            rgbData.add(RgbColor.Blank)
        }

        val frameData = RgbFrameData(0, rgbData)
        val optionsBuilder = RgbFrameOptionsBuilder()
        optionsBuilder.setClearBuffer()
        val options = optionsBuilder.build()
        val frame = serializer.encode(frameData, options)
        withContext(Dispatchers.IO) {
            client?.send(frame)?.get(1, TimeUnit.SECONDS)
        }
    }

    private fun syncClientTime() {
        // Don't sync more often than every 5 seconds
        if (timeSinceLastSync > 1000 * 5) {
            val requestTimestamp = timeHelper.millisSinceEpoch()
            val clientTime = configClient.getClientTime(clientEntity)
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

    private fun setupWebSocket() {
        val pattern = Regex("https?")
        val websocketAddress = clientEntity.address!!.replace(pattern, "ws")
        val uri = UriBuilder.of(websocketAddress)
            .port(clientEntity.wsPort!!)
            .path("test")
            .build()
        val future = CompletableFuture<LedStripWebSocketClient>()
        val clientPublisher = webSocketClient.connect(LedStripWebSocketClient::class.java, uri)
        clientPublisher.subscribe(object : Subscriber<LedStripWebSocketClient> {
            override fun onSubscribe(s: Subscription?) {
                s?.request(1)
            }

            override fun onError(t: Throwable?) {
                state = WebSocketState.DisconnectedIdle
                future.completeExceptionally(t)
            }

            override fun onComplete() {}

            override fun onNext(client: LedStripWebSocketClient?) {
                state = WebSocketState.ConnectedIdle
                client?.registerOnDisconnectedCallback({
                    if (state != WebSocketState.InsufficientData) {
                        state = WebSocketState.DisconnectedIdle
                    }
                })
                future.complete(client)
            }
        })

        client = future.get(5, TimeUnit.SECONDS)
    }

    override fun dispose() {
        shouldRun = false
        client?.unregisterOnDisconnectedCallback()
        client?.close()
    }

    fun onDataUpdate() {
        client?.close()
        state = WebSocketState.InsufficientData
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebSocketJob::class.java)
    }
}