package io.cyborgsquirrel.jobs.streaming.nightdriver

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.lighting.effect_trigger.service.TriggerManager
import io.cyborgsquirrel.jobs.streaming.ClientStreamingJob
import io.cyborgsquirrel.jobs.streaming.StreamingJobState
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.model.RgbFrameData
import io.cyborgsquirrel.lighting.rendering.LightEffectRenderer
import io.cyborgsquirrel.jobs.streaming.serialization.NightDriverFrameDataSerializer
import io.cyborgsquirrel.jobs.streaming.serialization.NightDriverSocketResponseDeserializer
import io.cyborgsquirrel.util.time.TimeHelper
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.Socket
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
    private val deserializer = NightDriverSocketResponseDeserializer()

    // Time tracking
    private var timestampMillis = 0L
    private val bufferTimeMillis = 250L
    private val deserializeTcpResponseIntervalMillis = 100L
    private var lastTcpResponseDeserializedAtMillis = 0L

    // State/logic
    private var exponentialReconnectionBackoffValue = 1
    private val exponentialReconnectionBackoffValueMax = 8
    private val fps = 35
    private var shouldRun = true
    private var state = StreamingJobState.SetupIncomplete
    private var lastResponse: NightDriverSocketResponse? = null

    fun getLatestResponse() = lastResponse

    override fun getCurrentState() = state

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
            if (state != StreamingJobState.SetupIncomplete && socket?.isConnected == false) {
                delay(250)
                state = StreamingJobState.DisconnectedIdle
            }

            when (state) {
                StreamingJobState.SetupIncomplete -> {
                    val clientOptional = clientRepository.findByUuid(clientEntity.uuid!!)
                    if (clientOptional.isPresent) {
                        clientEntity = clientOptional.get()
                        if (clientEntity.strips.isNotEmpty()) {
                            val stripEntity = clientEntity.strips.first()
                            // Night Driver manages the power limit so 0 is acceptable here
                            strip = LedStripModel(
                                stripEntity.name!!,
                                stripEntity.uuid!!,
                                stripEntity.pin!!,
                                stripEntity.length!!,
                                stripEntity.height,
                                stripEntity.blendMode!!,
                                stripEntity.brightness!!,
                            )
                            state = StreamingJobState.DisconnectedIdle
                        } else {
                            delay(5000)
                        }
                    } else {
                        delay(5000)
                    }
                }

                StreamingJobState.ConnectedIdle -> {
                    exponentialReconnectionBackoffValue = 1
                    timestampMillis = timeHelper.millisSinceEpoch() + bufferTimeMillis
                    state = StreamingJobState.RenderingEffect
                }

                StreamingJobState.DisconnectedIdle -> {
                    logger.info("Client $clientEntity disconnected. Attempting to reconnect...")
                    setupSocket()
                }

                StreamingJobState.SettingsSync -> {
                    // Not supported by NightDriver - we should never end up in this state
                    throw Exception("Unexpected state! $state")
                }

                StreamingJobState.BufferFullWaiting -> {
                    // Not supported by NightDriver - we should never end up in this state
                    throw Exception("Unexpected state! $state")
                }

                StreamingJobState.WaitingForConnection -> {
                    // Not supported by NightDriver - we should never end up in this state
                    throw Exception("Unexpected state! $state")
                }

                StreamingJobState.TimeSyncRequired -> {
                    // Not supported by NightDriver - we should never end up in this state
                    throw Exception("Unexpected state! $state")
                }

                StreamingJobState.RenderingEffect -> {
                    triggerManager.processTriggers()
                    val frameOptional = renderer.renderFrame(strip.getUuid(), 0)
                    if (frameOptional.isEmpty) {
                        delay(150)
                        timestampMillis = timeHelper.millisSinceEpoch() + bufferTimeMillis
                    } else {
                        // Time tracking - fast-forward timestamp if it is the same as the current time or in the past.
                        val now = timeHelper.millisSinceEpoch()
                        if (timestampMillis <= now) {
                            logger.info("Timestamp is less than or equal to now, catching it up.")
                            timestampMillis = now + bufferTimeMillis
                        } else if (timestampMillis - (bufferTimeMillis * 2) > now) {
                            // Wait for the duration of one frame so we don't overload the NightDriver client
                            delay(1000 / fps.toLong())
                        } else if (lastResponse != null) {
                            if (lastResponse!!.newestPacketMillis() > bufferTimeMillis) {
                                // Wait for the duration of one frame so we don't overload the NightDriver client
                                delay(1000 / fps.toLong())
                            }
                        }

                        timestampMillis += 1000 / fps

                        // Assemble RGB data
                        val frame = frameOptional.get()
                        val rgbData = frame.frameData
                        val frameData = RgbFrameData(timestampMillis, rgbData)

                        // Serialize and send frame - options are not supported for NightDriver
                        val encodedFrame = serializer.encode(frameData, strip.getPin().toInt())
                        sendSocketFrame(encodedFrame)

                        val delayMillis = max((1000 / fps) - 10L, 1L)
                        delay(delayMillis)
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

    private suspend fun sendSocketFrame(frame: ByteArray) {
        try {
            withContext(Dispatchers.IO) {
                val availableBytes = socket?.getInputStream()?.available()?.toLong()
                if (availableBytes != null && availableBytes > 0L) {
                    val now = timeHelper.millisSinceEpoch()
                    if (now - lastTcpResponseDeserializedAtMillis >= deserializeTcpResponseIntervalMillis) {
                        lastTcpResponseDeserializedAtMillis = now
                        val bytes = ByteArray(NightDriverSocketResponse.SIZE_IN_BYTES)
                        socket?.getInputStream()?.read(bytes)
                        lastResponse = deserializer.deserialize(bytes)
                        logger.debug("Night Driver client {} status {}", clientEntity, lastResponse)
                    } else {
                        socket?.getInputStream()?.skip(availableBytes)
                    }
                }

                socket?.getOutputStream()?.write(frame)
                socket?.getOutputStream()?.flush()
            }
        } catch (sockEx: SocketException) {
            if (state != StreamingJobState.SetupIncomplete) {
                state = StreamingJobState.DisconnectedIdle
            }
        }
    }

    private suspend fun setupSocket() {
        val httpSlashPattern = Regex("^(http|https)://")
        val httpSlashPatternResult = httpSlashPattern.find(clientEntity.address!!)
        val host = if (httpSlashPatternResult?.groups?.isNotEmpty() == true) {
            clientEntity.address?.replace(httpSlashPattern, "")
        } else {
            clientEntity.address
        }

        withContext(Dispatchers.IO) {
            try {
                socket?.close()
            } catch (_: Exception) {
            }

            socket = Socket()
            socket?.connect(InetSocketAddress(host, clientEntity.wsPort!!), 5000)
        }

        if (socket?.isConnected == true) {
            state = StreamingJobState.ConnectedIdle
        } else {
            if (state != StreamingJobState.SetupIncomplete) {
                state = StreamingJobState.DisconnectedIdle
            }
        }
    }

    override fun dispose() {
        shouldRun = false
        socket?.close()
    }

    override fun onDataUpdate() {
        socket?.close()
        state = StreamingJobState.SetupIncomplete
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NightDriverSocketJob::class.java)
    }
}