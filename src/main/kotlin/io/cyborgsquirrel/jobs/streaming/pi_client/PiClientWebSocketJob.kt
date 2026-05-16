package io.cyborgsquirrel.jobs.streaming.pi_client

import io.cyborgsquirrel.clients.config.pi_client.PiClientSettings
import io.cyborgsquirrel.clients.config.pi_client.PiClientStripConfig
import io.cyborgsquirrel.clients.config.pi_client.PiConfigClient
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.jobs.streaming.ClientStreamingJob
import io.cyborgsquirrel.jobs.streaming.model.PiStreamingJobState
import io.cyborgsquirrel.jobs.streaming.model.StreamingJobStatus
import io.cyborgsquirrel.jobs.streaming.serialization.PiFrameDataSerializer
import io.cyborgsquirrel.jobs.streaming.util.ClientTimeSync
import io.cyborgsquirrel.lighting.effect_trigger.service.TriggerManager
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.service.LightEffectRegistry
import reactor.core.Disposable
import io.cyborgsquirrel.lighting.model.*
import io.cyborgsquirrel.lighting.rendering.LightEffectRenderer
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameSegmentModel
import io.cyborgsquirrel.util.time.TimeHelper
import io.micronaut.http.uri.UriBuilder
import io.micronaut.websocket.WebSocketClient
import kotlinx.coroutines.*
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.slf4j.LoggerFactory

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
    private val clientRepository: LedStripClientRepository,
    private val timeHelper: TimeHelper,
    private val piConfigClient: PiConfigClient,
    private var clientEntity: LedStripClientEntity,
    private var activeLightEffectService: LightEffectRegistry,
) : ClientStreamingJob {

    // Pi WebSocket client
    private var client: PiWebSocketClient? = null
    private var effectsSubscription: Disposable? = null

    // Client data — written from both the coroutine and the onUpdate listener callback
    @Volatile
    private var strips: List<LedStripModel> = emptyList()

    // Serialization
    private val serializer = PiFrameDataSerializer()

    // Time tracking
    private val clientTimeSync = ClientTimeSync(timeHelper)
    private val timeDesyncToleranceMillis = 5
    private val timeSinceLastSync: Long
        get() = timeHelper.millisSinceEpoch() - clientTimeSync.lastTimeSyncPerformedAt
    private var lastKeepaliveFrameSentAt: Long? = null
    private var timestampMillis = 0L
    private var sleepMillis = 0L
    private var lastSeenAt = 0L

    // State/logic — status written from both the coroutine and WebSocket/listener callbacks
    @Volatile
    private var status = StreamingJobStatus.SetupIncomplete
    private var exponentialReconnectionBackoffValue = 1
    private val exponentialReconnectionBackoffValueMax = 8
    private val fps get() = clientEntity.fps
    private val bufferTimeInMilliseconds = 500L
    private var shouldRun = true
    private var settingsSyncRequired = true

    /**
     * Starts the job which will run in the background using a Kotlin Coroutine.
     * Returns the Job instance.
     */
    override fun start(scope: CoroutineScope): Job {
        effectsSubscription = activeLightEffectService.updates.subscribe { onEffectsUpdate(it) }
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
            val responseBytes = client?.responseQueue?.poll()
            if (responseBytes != null) {
                handleResponse(responseBytes.toPiClientResponse(timeHelper))
            }
            when (status) {
                StreamingJobStatus.SetupIncomplete -> {
                    val clientOptional = clientRepository.findByUuid(clientEntity.uuid)
                    if (clientOptional.isPresent) {
                        clientEntity = clientOptional.get()
                        if (clientEntity.strips.isNotEmpty()) {
                            strips = activeLightEffectService.getEffectsForClient(clientEntity.uuid).map { it.strip }
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
                    status = when {
                        settingsSyncRequired -> StreamingJobStatus.SettingsSync
                        clientTimeSync.lastTimeSyncPerformedAt == 0L -> StreamingJobStatus.TimeSyncRequired
                        else -> StreamingJobStatus.RenderingEffect
                    }
                }

                StreamingJobStatus.Offline -> {
                    logger.info("Client $clientEntity disconnected. Attempting to reconnect...")
                    delay(1000)
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

                    timestampMillis = timeHelper.millisSinceEpoch() + clientTimeSync.clientTimeOffset
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
                                timeHelper.dateTimeFromMillis(timestampMillis)
                            })"
                        )
                        status = StreamingJobStatus.TimeSyncRequired
                    } else {
                        triggerManager.processTriggers()
                        val frames = renderer.renderFrames(strips, clientEntity.uuid)
                        if (frames.isEmpty()) {
                            sendKeepaliveIfDue()
                            timestampMillis = timeHelper.millisSinceEpoch() + clientTimeSync.clientTimeOffset
                        } else {
                            // Reset so the strip is cleared immediately when effects stop
                            lastKeepaliveFrameSentAt = null
                            timestampMillis += 1000 / fps
                            val serializedFrames = serializeFrames(frames)
                            sendFrames(serializedFrames)

                            val nowPlusBufferMillis = timeHelper.millisSinceEpoch() + bufferTimeInMilliseconds
                            if (nowPlusBufferMillis < timestampMillis) {
                                sleepMillis = timestampMillis - nowPlusBufferMillis
                                status = StreamingJobStatus.BufferFullWaiting
                            }
                        }
                    }
                }
            }
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Error while processing state $status", ex)
            if (status == StreamingJobStatus.WaitingForConnection) {
                status = StreamingJobStatus.Offline
            }
            delay((2 shl exponentialReconnectionBackoffValue) * 1000L)
            if (exponentialReconnectionBackoffValue < exponentialReconnectionBackoffValueMax) exponentialReconnectionBackoffValue++
        }
    }

    private fun serializeFrames(frames: List<RenderedFrameSegmentModel>): List<ByteArray> {
        return frames.map { frame ->
            serializer.encode(
                RgbFrameData(timestampMillis, frame.frameData),
                frame.strip.pin,
                RgbFrameOptionsBuilder().build(),
            )
        }
    }

    private suspend fun sendFrames(frames: List<ByteArray>) {
        frames.forEach { frame ->
            withContext(Dispatchers.IO) { client?.send(frame) }
        }
    }

    private suspend fun sendKeepaliveIfDue() {
        val now = timeHelper.millisSinceEpoch()
        val oneMinuteMillis = 60_000L
        if (lastKeepaliveFrameSentAt == null || now - lastKeepaliveFrameSentAt!! >= oneMinuteMillis) {
            logger.info("Sending keep-alive frame to $clientEntity")
            sendClearFrame()
            lastKeepaliveFrameSentAt = now
        }
    }

    private suspend fun doClientSettingsSync() {
        logger.info("Syncing settings with $clientEntity")
        val strip = getStrip()
        if (strip == null) {
            logger.warn("No strip found for $clientEntity during settings sync")
            status = StreamingJobStatus.ConnectedIdle
            return
        }
        settingsSyncRequired = false

        val clientStripConfigs = piConfigClient.getStripConfigs(clientEntity)
        val clientSettings = piConfigClient.getClientSettings(clientEntity)
        val serverConfig = PiClientStripConfig(
            strip.uuid, strip.pin, strip.length, strip.brightness, clientEntity.colorOrder
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
                && clientSettings.fadeTimeoutMillis == clientEntity.fadeTimeoutMillis
        if (!clientSettingsMatch) {
            logger.info("Settings out of sync for $clientEntity - server config: $serverConfig")
            piConfigClient.updateClientSettings(
                clientEntity,
                PiClientSettings(clientEntity.powerLimit ?: 0, clientEntity.fadeTimeoutMillis)
            )
        }

        logger.info("Checking Pi client version...")
        updateClientVersion()

        status = StreamingJobStatus.ConnectedIdle
    }

    private fun updateLastSeenAt(currentTimeAsMillis: Long) {
        val oneMinuteInMillis = 60 * 1000
        if (currentTimeAsMillis - lastSeenAt > oneMinuteInMillis) {
            val clientEntityOptional = clientRepository.findById(clientEntity.id)
            if (clientEntityOptional.isPresent) {
                val ce = clientEntityOptional.get()
                ce.lastSeenAt = currentTimeAsMillis
                lastSeenAt = currentTimeAsMillis
                clientEntity = clientRepository.update(ce)
            }
        }
    }

    private suspend fun updateClientVersion() {
        val version = piConfigClient.getClientVersion(clientEntity).version
        logger.info("Pi client $clientEntity is running version $version")
        val clientEntityOptional = clientRepository.findById(clientEntity.id)
        if (clientEntityOptional.isPresent) {
            val ce = clientEntityOptional.get()
            ce.firmwareVersion = version
            clientEntity = clientRepository.update(ce)
        }
    }

    private suspend fun sendClearFrame() {
        val strip = getStrip() ?: return
        val rgbData = List(strip.length) { RgbColor.Blank }
        val optionsBuilder = RgbFrameOptionsBuilder()
        optionsBuilder.setClearBuffer()
        val frame = serializer.encode(RgbFrameData(0, rgbData), strip.pin, optionsBuilder.build())
        withContext(Dispatchers.IO) { client?.send(frame) }
    }

    private suspend fun setupSocket() {
        val httpPattern = Regex("^(http|https)")
        val httpPatternResult = httpPattern.find(clientEntity.address)
        val websocketAddress = if (httpPatternResult?.groups?.isNotEmpty() == true) {
            clientEntity.address.replace(httpPattern, "ws")
        } else {
            "ws://${clientEntity.address}"
        }
        val uri = UriBuilder.of(websocketAddress).port(clientEntity.wsPort).build()
        val clientPublisher = webSocketClient.connect(PiWebSocketClient::class.java, uri)

        client = withTimeout(5000L) {
            suspendCancellableCoroutine { cont ->
                clientPublisher.subscribe(object : Subscriber<PiWebSocketClient> {
                    override fun onSubscribe(s: Subscription?) {
                        s?.request(1)
                        cont.invokeOnCancellation { s?.cancel() }
                    }

                    override fun onError(t: Throwable?) {
                        status = StreamingJobStatus.Offline
                        cont.resumeWith(Result.failure(t ?: Exception("WebSocket connection failed")))
                    }

                    override fun onComplete() {
                        if (cont.isActive) {
                            status = StreamingJobStatus.Offline
                            cont.resumeWith(Result.failure(Exception("WebSocket connection closed without connecting")))
                        }
                    }

                    override fun onNext(piClient: PiWebSocketClient?) {
                        status = StreamingJobStatus.ConnectedIdle
                        settingsSyncRequired = true
                        piClient?.registerOnDisconnectedCallback {
                            if (status != StreamingJobStatus.SetupIncomplete) {
                                status = StreamingJobStatus.Offline
                            }
                        }
                        cont.resumeWith(Result.success(piClient))
                    }
                })
            }
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
            is SingleLedStripModel -> firstStrip
            is LedStripPoolModel -> firstStrip.strips.first { it.clientUuid == clientEntity.uuid }
        }
    }

    internal fun onEffectsUpdate(newEffects: List<ActiveLightEffect>) {
        val matchingStrips = newEffects.filter {
            val strip = it.strip
            val clientUuid = clientEntity.uuid
            when (strip) {
                is SingleLedStripModel -> strip.clientUuid == clientUuid
                is LedStripPoolModel -> strip.clientUuids().contains(clientUuid)
            }
        }.map { it.strip }

        if (strips != matchingStrips) {
            strips = matchingStrips
            status = StreamingJobStatus.SettingsSync
        }
    }

    private fun handleResponse(response: PiClientResponse?) {
        when (response) {
            is PiClientResponse.BackpressureError -> {
                sleepMillis = bufferTimeInMilliseconds / 2
                status = StreamingJobStatus.BufferFullWaiting
            }

            is PiClientResponse.NoResponse,
            is PiClientResponse.GenericError,
            is PiClientResponse.UnknownType -> {
                logger.warn("Unexpected response from $clientEntity: $response - reconnecting")
                status = StreamingJobStatus.Offline
                client?.close()
            }

            is PiClientResponse.BufferStatus, null -> Unit
        }
    }

    override fun dispose() {
        effectsSubscription?.dispose()
        shouldRun = false
        client?.unregisterOnDisconnectedCallback()
        client?.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PiClientWebSocketJob::class.java)
    }
}
