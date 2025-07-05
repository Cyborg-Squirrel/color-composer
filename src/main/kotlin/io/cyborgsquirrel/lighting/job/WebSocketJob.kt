package io.cyborgsquirrel.lighting.job

import io.cyborgsquirrel.clients.config.ConfigClient
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.lighting.client.LedStripWebSocketClient
import io.cyborgsquirrel.lighting.effect_trigger.service.TriggerManager
import io.cyborgsquirrel.lighting.model.*
import io.cyborgsquirrel.lighting.rendering.LightEffectRenderer
import io.cyborgsquirrel.lighting.serialization.NightDriverFrameDataSerializer
import io.cyborgsquirrel.lighting.serialization.PiFrameDataSerializer
import io.cyborgsquirrel.util.time.TimeHelper
import io.micronaut.http.uri.UriBuilder
import io.micronaut.websocket.WebSocketClient
import kotlinx.coroutines.*
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.slf4j.LoggerFactory
import java.net.Socket
import java.net.SocketException
import java.sql.Timestamp
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

    // Pi WebSocket client
    private var client: LedStripWebSocketClient? = null

    // NightDriver TCP connection
    private var socket: Socket? = null

    // Client data
    private lateinit var strip: LedStripModel
    private val timeSyncSupported: Boolean
        get() = clientEntity.clientType == ClientType.Pi

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
    private val bufferTimeInSeconds = 1L
    private var shouldRun = true
    private var state = WebSocketState.InsufficientData

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
            // Check if NightDriver socket is still connected
            if (state != WebSocketState.InsufficientData && socket?.isConnected == false) {
                delay(250)
                state = WebSocketState.DisconnectedIdle
            }

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
                                stripEntity.pin!!,
                                stripEntity.length!!,
                                stripEntity.height,
                                stripEntity.blendMode!!
                            )
                            timestampMillis = timeHelper.millisSinceEpoch() + (1000 / fps) + clientTimeOffset
                            state = WebSocketState.WaitingForConnection
                            setupSocket()
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
                    state = if (timeSyncSupported && lastTimeSyncPerformedAt == 0L) {
                        WebSocketState.TimeSyncRequired
                    } else {
                        WebSocketState.RenderingEffect
                    }
                }

                WebSocketState.DisconnectedIdle -> {
                    try {
                        logger.info("Client $clientEntity disconnected. Attempting to reconnect...")
                        state = WebSocketState.WaitingForConnection
                        setupSocket()
                    } catch (ex: Exception) {
                        logger.info("Unable to connect to client $clientEntity")
                        if (exponentialReconnectionBackoffValue <= exponentialReconnectionBackoffValueMax) exponentialReconnectionBackoffValue++
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
                    logger.info("New timestamp ${timeHelper.dateTimeFromMillis(timestampMillis)} millis $timestampMillis")

                    // If we disconnect during the time sync don't set the state to connected
                    if (state != WebSocketState.DisconnectedIdle) {
                        state = WebSocketState.RenderingEffect
                    }
                }

                WebSocketState.RenderingEffect -> {
                    val currentTimeAsMillis = timeHelper.millisSinceEpoch()
                    val timeDesynced =
                        timestampMillis + timeDesyncToleranceMillis < currentTimeAsMillis + clientTimeOffset
                    if (timeSyncSupported && timeDesynced) {
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
                        val frameOptional = renderer.renderFrame(strip.getUuid(), 0)
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
                            val encodedFrame = serializeFrame(frameData, strip.getPin(), options)

                            when (clientEntity.clientType!!) {
                                ClientType.Pi -> withContext(Dispatchers.IO) {
                                    client?.send(encodedFrame)?.get(1, TimeUnit.SECONDS)
                                }

                                ClientType.NightDriver -> sendSocketFrame(encodedFrame)
                            }

                            // Slow down to ensure we only buffer the specified amount of time into the future.
                            val nowPlusBufferSeconds =
                                Timestamp.from(Instant.now().plusSeconds(bufferTimeInSeconds)).time
                            if (nowPlusBufferSeconds < timestampMillis) {
                                sleepMillis = timestampMillis - nowPlusBufferSeconds
                                state = WebSocketState.BufferFullWaiting
                            } else if (frame.allEffectsPaused) {
                                // Wait for the duration of one frame to reduce time spent rendering/sending frames
                                // if all effects are paused.
                                sleepMillis = (1000 / fps).toLong()
                                timestampMillis += sleepMillis
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
            if (exponentialReconnectionBackoffValue <= exponentialReconnectionBackoffValueMax) exponentialReconnectionBackoffValue++
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
        val frame = serializeFrame(frameData, strip.getPin(), options)

        when (clientEntity.clientType!!) {
            ClientType.Pi -> withContext(Dispatchers.IO) {
                client?.send(frame)?.get(1, TimeUnit.SECONDS)
            }

            ClientType.NightDriver -> sendSocketFrame(frame)
        }
    }

    private suspend fun sendSocketFrame(frame: ByteArray) {
        try {
            withContext(Dispatchers.IO) {
                socket?.getOutputStream()?.write(frame)
            }
        } catch (sockEx: SocketException) {
            if (state != WebSocketState.InsufficientData) {
                state = WebSocketState.DisconnectedIdle
            }
        }
    }

    private fun serializeFrame(frameData: RgbFrameData, pin: String, options: RgbFrameOptions): ByteArray {
        return when (clientEntity.clientType) {
            ClientType.Pi -> {
                val serializer = PiFrameDataSerializer()
                serializer.encode(frameData, pin, options)
            }

            ClientType.NightDriver -> {
                val serializer = NightDriverFrameDataSerializer()
                serializer.encode(frameData, pin.toInt())
            }

            null -> throw Exception("No clientType specified!")
        }
    }

    private fun syncClientTime() {
        // Don't sync more often than every 3 seconds if we end up looping on time sync for some reason
        if (timeSinceLastSync > 1000 * 3) {
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

    private fun setupSocket() {
        when (clientEntity.clientType!!) {
            ClientType.Pi -> {
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

            ClientType.NightDriver -> {
                val httpSlashPattern = Regex("^(http|https)://")
                val httpSlashPatternResult = httpSlashPattern.find(clientEntity.address!!)
                val host = if (httpSlashPatternResult?.groups?.isNotEmpty() == true) {
                    clientEntity.address?.replace(httpSlashPattern, "")
                } else {
                    clientEntity.address
                }

                socket = Socket(host, clientEntity.wsPort!!)
                if (socket?.isConnected == true) {
                    state = WebSocketState.ConnectedIdle
                } else {
                    if (state != WebSocketState.InsufficientData) {
                        state = WebSocketState.DisconnectedIdle
                    }
                }
            }
        }
    }

    override fun dispose() {
        shouldRun = false
        client?.unregisterOnDisconnectedCallback()
        client?.close()
        socket?.close()
    }

    fun onDataUpdate() {
        client?.close()
        socket?.close()
        state = WebSocketState.InsufficientData
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebSocketJob::class.java)
    }
}