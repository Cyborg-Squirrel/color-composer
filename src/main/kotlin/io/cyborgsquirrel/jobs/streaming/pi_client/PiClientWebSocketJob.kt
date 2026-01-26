package io.cyborgsquirrel.jobs.streaming.pi_client

import io.cyborgsquirrel.clients.config.pi_client.PiClientSettings
import io.cyborgsquirrel.clients.config.pi_client.PiClientStripConfig
import io.cyborgsquirrel.clients.config.pi_client.PiConfigClient
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.jobs.streaming.ClientStreamingJob
import io.cyborgsquirrel.jobs.streaming.model.PiStreamingJobState
import io.cyborgsquirrel.jobs.streaming.model.StreamingJobStatus
import io.cyborgsquirrel.jobs.streaming.serialization.PiFrameDataSerializer
import io.cyborgsquirrel.jobs.streaming.util.ClientTimeSync
import io.cyborgsquirrel.lighting.effect_trigger.service.TriggerManager
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.service.ActiveLightEffectChangeListener
import io.cyborgsquirrel.lighting.effects.service.ActiveLightEffectService
import io.cyborgsquirrel.lighting.model.*
import io.cyborgsquirrel.lighting.rendering.LightEffectRenderer
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
 * job goes to the [StreamingJobStatus.BufferFullWaiting] state to wait for the Pi to render one or more frames.
 */
class PiClientWebSocketJob(
    private val webSocketClient: WebSocketClient,
    private val renderer: LightEffectRenderer,
    private val triggerManager: TriggerManager,
    private val clientRepository: H2LedStripClientRepository,
    private val timeHelper: TimeHelper,
    private val piConfigClient: PiConfigClient,
    private var clientEntity: LedStripClientEntity,
    private var activeLightEffectService: ActiveLightEffectService,
) : ClientStreamingJob, ActiveLightEffectChangeListener {

    // Pi WebSocket client
    private var client: PiWebSocketClient? = null

    // Client data
    private val strips = mutableListOf<LedStripModel>()

    // Serialization
    private val serializer = PiFrameDataSerializer()

    // Time tracking
    private val clientTimeSync = ClientTimeSync(timeHelper)
    private val timeDesyncToleranceMillis = 5
    private val timeSinceLastSync: Long
        get() = timeHelper.millisSinceEpoch() - clientTimeSync.lastTimeSyncPerformedAt
    private var lastKeepaliveFrameTimestamp = LocalDateTime.of(0, 1, 1, 0, 0)
    private var timestampMillis = 0L
    private var sleepMillis = 0L
    private var lastSeenAt = 0L

    // State/logic
    private var exponentialReconnectionBackoffValue = 1
    private val exponentialReconnectionBackoffValueMax = 8
    private val fps = 35
    private val bufferTimeInMilliseconds = 500L
    private var shouldRun = true
    private var settingsSyncRequired = true
    private var status = StreamingJobStatus.SetupIncomplete

    /**
     * Starts the job which will run in the background using a Kotlin Coroutine.
     * Returns the Job instance.
     */
    override fun start(scope: CoroutineScope): Job {
        activeLightEffectService.addListener(this)
        return scope.launch {
            logger.info("Start")
            while (isActive && shouldRun) {
                processState()
            }
            logger.info("Done")
        }
    }

    override fun getCurrentState() = PiStreamingJobState(status)

    private suspend fun processState() {
        try {
            when (status) {
                StreamingJobStatus.SetupIncomplete -> {
                    val clientOptional = clientRepository.findByUuid(clientEntity.uuid!!)
                    if (clientOptional.isPresent) {
                        clientEntity = clientOptional.get()
                        if (clientEntity.strips.isNotEmpty()) {
                            val newStrips =
                                activeLightEffectService.getEffectsForClient(clientEntity.uuid!!).map { it.strip }
                            strips.clear()
                            strips.addAll(newStrips)
                            timestampMillis =
                                timeHelper.millisSinceEpoch() + (1000 / fps) + clientTimeSync.clientTimeOffset
                            status = StreamingJobStatus.WaitingForConnection
                            setupSocket()
                        } else {
                            delay(5000)
                        }
                    } else {
                        dispose()
                    }
                }

                StreamingJobStatus.WaitingForConnection -> {
                    delay(50)
                }

                StreamingJobStatus.SettingsSync -> doClientSettingsSync()

                StreamingJobStatus.ConnectedIdle -> {
                    exponentialReconnectionBackoffValue = 1
                    status = if (settingsSyncRequired) {
                        StreamingJobStatus.SettingsSync
                    } else if (clientTimeSync.lastTimeSyncPerformedAt == 0L) {
                        StreamingJobStatus.TimeSyncRequired
                    } else {
                        StreamingJobStatus.RenderingEffect
                    }
                }

                StreamingJobStatus.Offline -> {
                    logger.info("Client $clientEntity disconnected. Attempting to reconnect...")
                    settingsSyncRequired = true
                    status = StreamingJobStatus.WaitingForConnection
                    setupSocket()
                }

                StreamingJobStatus.BufferFullWaiting -> {
                    delay(sleepMillis)
                    status = StreamingJobStatus.RenderingEffect
                }

                StreamingJobStatus.TimeSyncRequired -> {
                    // Don't sync more often than every 3 seconds if we end up looping on time sync for some reason
                    if (timeSinceLastSync > 1000 * 3) {
                        clientTimeSync.doTimeSync { piConfigClient.getClientTime(clientEntity).millisSinceEpoch }
                    }

                    timestampMillis = timeHelper.millisSinceEpoch() // clientTimeSync.clientTimeOffset
                    logger.info("New timestamp ${timeHelper.dateTimeFromMillis(timestampMillis)} millis $timestampMillis")

                    // If we disconnect during the time sync don't set the state to rendering
                    if (status != StreamingJobStatus.Offline) {
                        status = StreamingJobStatus.RenderingEffect
                    }
                }

                StreamingJobStatus.RenderingEffect -> {
                    val currentTimeAsMillis = timeHelper.millisSinceEpoch()
                    val timeDesynced =
                        timestampMillis + timeDesyncToleranceMillis < currentTimeAsMillis + clientTimeSync.clientTimeOffset
                    updateLastSeenAt(currentTimeAsMillis)
                    if (timeDesynced) {
                        logger.info(
                            "Re-syncing time with client $client - (client time offset ${clientTimeSync.clientTimeOffset}ms frame timestamp: ${
                                timeHelper.dateTimeFromMillis(
                                    timestampMillis
                                )
                            })"
                        )
                        status = StreamingJobStatus.TimeSyncRequired
                    } else {
                        triggerManager.processTriggers()
                        val frames = renderer.renderFrames(strips, clientEntity.uuid!!)
                        if (frames.isEmpty()) {
                            // Send a keep-alive frame to clear the strip and prevent the WebSocket from timing out
                            if (lastKeepaliveFrameTimestamp.plusMinutes(1).isBefore(LocalDateTime.now())) {
                                logger.info("Sending keep-alive frame to $clientEntity")
                                sendClearFrame()
                                lastKeepaliveFrameTimestamp = LocalDateTime.now()
                            } else {
                                delay(250)
                            }

                            timestampMillis = timeHelper.millisSinceEpoch() + clientTimeSync.clientTimeOffset
                        } else {
                            // Time tracking - reset last keep-alive timestamp to ensure if effects are stopped the empty
                            // frame logic will immediately send sendClearFrame() to clear the strip.
                            lastKeepaliveFrameTimestamp = LocalDateTime.of(0, 1, 1, 0, 0)
                            timestampMillis += 1000 / fps

                            frames.forEach { frame ->
                                val pin = frame.strip.pin
                                val rgbData = frame.frameData
                                val frameData = RgbFrameData(timestampMillis, rgbData)

                                // Build options - clear the frame buffer if all effects are paused, this makes the LED strip more responsive to play/pause commands
                                val optionsBuilder = RgbFrameOptionsBuilder()
                                val options = optionsBuilder.build()

                                // Serialize and send frame
                                val encodedFrame = serializer.encode(frameData, pin, options)

                                withContext(Dispatchers.IO) {
                                    client?.send(encodedFrame)?.get(1, TimeUnit.SECONDS)
                                }
                            }

                            // Slow down to ensure we only buffer the specified amount of time into the future.
                            val nowPlusBufferMillis =
                                Timestamp.from(Instant.now().plusMillis(bufferTimeInMilliseconds)).time
                            if (nowPlusBufferMillis < timestampMillis) {
                                sleepMillis = timestampMillis - nowPlusBufferMillis
                                status = StreamingJobStatus.BufferFullWaiting
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            logger.error("Error $ex while processing state $status")
            ex.printStackTrace()
            delay((2 shl exponentialReconnectionBackoffValue) * 1000L)
            if (exponentialReconnectionBackoffValue < exponentialReconnectionBackoffValueMax) exponentialReconnectionBackoffValue++
        }
    }

    private suspend fun doClientSettingsSync() {
        logger.info("Syncing settings with $clientEntity")
        settingsSyncRequired = false
        val clientStripConfigs = piConfigClient.getStripConfigs(clientEntity)
        val clientSettings = piConfigClient.getClientSettings(clientEntity)
        val strip = getStrip()
        val serverConfig = PiClientStripConfig(
            strip!!.uuid, strip.pin, strip.length, strip.brightness, clientEntity.colorOrder!!
        )

        val stripConfigMatch =
            clientStripConfigs.configList.size == 1 && clientStripConfigs.configList.first() == serverConfig

        if (!stripConfigMatch) {
            logger.info("Strip settings out of sync for $clientEntity - server config: $serverConfig")

            for (config in clientStripConfigs.configList) {
                piConfigClient.deleteStripConfig(clientEntity, config)
            }
            piConfigClient.addStripConfig(clientEntity, serverConfig)
        }

        val clientSettingsMatch = clientSettings.powerLimit == clientEntity.powerLimit

        if (!clientSettingsMatch) {
            logger.info("Settings out of sync for $clientEntity - server config: $serverConfig")

            piConfigClient.updateClientSettings(
                clientEntity, PiClientSettings(clientEntity.powerLimit ?: 0)
            )
        }

        status = StreamingJobStatus.ConnectedIdle
    }

    private fun updateLastSeenAt(currentTimeAsMillis: Long) {
        val oneMinuteInMillis = 60 * 1000
        val timeSinceLastSeenAtSaved = currentTimeAsMillis - lastSeenAt
        if (timeSinceLastSeenAtSaved > oneMinuteInMillis) {
            val clientEntityOptional = clientRepository.findById(clientEntity.id)
            if (clientEntityOptional.isPresent) {
                val ce = clientEntityOptional.get()
                ce.lastSeenAt = currentTimeAsMillis
                lastSeenAt = currentTimeAsMillis
                clientEntity = clientRepository.update(ce)
            }
        }
    }

    private suspend fun sendClearFrame() {
        val rgbData = mutableListOf<RgbColor>()
        val strip = getStrip()!!
        for (i in 0..<strip.length) {
            rgbData.add(RgbColor.Blank)
        }

        val frameData = RgbFrameData(0, rgbData)
        val optionsBuilder = RgbFrameOptionsBuilder()
        optionsBuilder.setClearBuffer()
        val options = optionsBuilder.build()
        val frame = serializer.encode(frameData, strip.pin, options)

        withContext(Dispatchers.IO) {
            client?.send(frame)?.get(1, TimeUnit.SECONDS)
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
        val future = CompletableFuture<PiWebSocketClient>()
        val clientPublisher = webSocketClient.connect(PiWebSocketClient::class.java, uri)
        clientPublisher.subscribe(object : Subscriber<PiWebSocketClient> {
            override fun onSubscribe(s: Subscription?) {
                s?.request(1)
            }

            override fun onError(t: Throwable?) {
                status = StreamingJobStatus.Offline
                future.completeExceptionally(t)
            }

            override fun onComplete() {}

            override fun onNext(client: PiWebSocketClient?) {
                status = StreamingJobStatus.ConnectedIdle
                client?.registerOnDisconnectedCallback({
                    if (status != StreamingJobStatus.SetupIncomplete) {
                        status = StreamingJobStatus.Offline
                    }
                })
                future.complete(client)
            }
        })

        client = withContext(Dispatchers.IO) {
            future.get(5, TimeUnit.SECONDS)
        }
    }

    /**
     * Pi clients only support one strip at a time. It may be a single strip or part of a group.
     */
    private fun getStrip(): SingleLedStripModel? {
        if (strips.isEmpty()) {
            return null
        }
        return when (val firstStrip = strips.first()) {
            is SingleLedStripModel -> {
                firstStrip
            }

            is LedStripPoolModel -> {
                firstStrip.strips.first { it.clientUuid == clientEntity.uuid }
            }
        }
    }

    override fun onUpdate(newEffects: List<ActiveLightEffect>) {
        val matchingStrips = newEffects.filter {
            val strip = it.strip
            val clientUuid = clientEntity.uuid!!
            when (strip) {
                is SingleLedStripModel -> {
                    strip.clientUuid == clientUuid
                }

                is LedStripPoolModel -> {
                    strip.clientUuids().contains(clientUuid)
                }
            }
        }.map { it.strip }

        if (strips != matchingStrips) {
            strips.clear()
            strips.addAll(matchingStrips)
            status = StreamingJobStatus.SettingsSync
        }
    }

    override fun dispose() {
        activeLightEffectService.removeLister(this)
        shouldRun = false
        client?.unregisterOnDisconnectedCallback()
        client?.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PiClientWebSocketJob::class.java)
    }
}