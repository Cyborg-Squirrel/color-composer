package io.cyborgsquirrel.lighting.job

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.lighting.effect_trigger.service.TriggerManager
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.model.RgbFrameData
import io.cyborgsquirrel.lighting.rendering.LightEffectRenderer
import io.cyborgsquirrel.lighting.serialization.NightDriverFrameDataSerializer
import io.cyborgsquirrel.util.time.TimeHelper
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketException
import kotlin.math.max

/**
 * Background job for streaming effects to clients
 */
class NightDriverSocketJob(
    private val renderer: LightEffectRenderer,
    private val triggerManager: TriggerManager,
    private val clientRepository: H2LedStripClientRepository,
    private val timeHelper: TimeHelper,
    private var clientEntity: LedStripClientEntity,
) : ClientStreamingJob {

    // NightDriver TCP connection
    private var socket: Socket? = null

    // Client data
    private lateinit var strip: LedStripModel

    // Serialization
    private val serializer = NightDriverFrameDataSerializer()

    // Time tracking
    private var timestampMillis = 0L
    private val bufferTimeMillis = 150L

    // State/logic
    private var exponentialReconnectionBackoffValue = 1
    private val exponentialReconnectionBackoffValueMax = 8
    private val fps = 30
    private var shouldRun = true
    private var state = WebSocketState.InsufficientData

    override fun start(scope: CoroutineScope): Job {
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
                            setupSocket()
                        } else {
                            delay(5000)
                        }
                    } else {
                        delay(5000)
                    }
                }

                WebSocketState.ConnectedIdle -> {
                    exponentialReconnectionBackoffValue = 1
                    timestampMillis = timeHelper.millisSinceEpoch() + bufferTimeMillis
                    state = WebSocketState.RenderingEffect
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
                    // Not supported by NightDriver - we should never end up in this state
                    throw Exception("Unexpected state! $state")
                }

                WebSocketState.WaitingForConnection -> {
                    // Not supported by NightDriver - we should never end up in this state
                    throw Exception("Unexpected state! $state")
                }

                WebSocketState.TimeSyncRequired -> {
                    // Not supported by NightDriver - we should never end up in this state
                    throw Exception("Unexpected state! $state")
                }

                WebSocketState.RenderingEffect -> {
                    triggerManager.processTriggers()
                    val frameOptional = renderer.renderFrame(strip.getUuid(), 0)
                    if (frameOptional.isEmpty) {
                        delay(150)
                        timestampMillis = timeHelper.millisSinceEpoch() + bufferTimeMillis
                    } else {
                        // Time tracking - fast-forward timestamp if it is the same as the current time or in the past.
                        val now = timeHelper.millisSinceEpoch()
                        if (now <= timestampMillis) {
                            logger.info("Timestamp is less than or equal to now")
                            timestampMillis = now + bufferTimeMillis
                        }

                        timestampMillis += 1000 / fps

                        // Assemble RGB data
                        val frame = frameOptional.get()
                        val rgbData = frame.frameData
                        val frameData = RgbFrameData(timestampMillis, rgbData)

                        // Serialize and send frame - options are not supported for NightDriver
                        val encodedFrame = serializer.encode(frameData, strip.getPin().toInt())
                        sendSocketFrame(encodedFrame)

                        val delayMillis = max((1000 / fps) - 7L, 0L)
                        delay(delayMillis)
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

    private suspend fun sendSocketFrame(frame: ByteArray) {
        try {
            withContext(Dispatchers.IO) {
                socket?.getOutputStream()?.write(frame)
                socket?.getOutputStream()?.flush()
            }
        } catch (sockEx: SocketException) {
            if (state != WebSocketState.InsufficientData) {
                state = WebSocketState.DisconnectedIdle
            }
        }
    }

    private fun setupSocket() {
        val httpSlashPattern = Regex("^(http|https)://")
        val httpSlashPatternResult = httpSlashPattern.find(clientEntity.address!!)
        val host = if (httpSlashPatternResult?.groups?.isNotEmpty() == true) {
            clientEntity.address?.replace(httpSlashPattern, "")
        } else {
            clientEntity.address
        }

        try {
            socket?.close()
        } catch (_: Exception) {
        }

        socket = Socket()
        socket?.connect(InetSocketAddress(host, clientEntity.wsPort!!), 5000)

        if (socket?.isConnected == true) {
            state = WebSocketState.ConnectedIdle
        } else {
            if (state != WebSocketState.InsufficientData) {
                state = WebSocketState.DisconnectedIdle
            }
        }
    }

    override fun dispose() {
        shouldRun = false
        socket?.close()
    }

    override fun onDataUpdate() {
        socket?.close()
        state = WebSocketState.InsufficientData
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NightDriverSocketJob::class.java)
    }
}