package io.cyborgsquirrel.jobs.streaming.nightdriver

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.jobs.streaming.ClientStreamingJob
import io.cyborgsquirrel.jobs.streaming.StreamingJobState
import io.cyborgsquirrel.jobs.streaming.serialization.NightDriverFrameDataSerializer
import io.cyborgsquirrel.jobs.streaming.serialization.NightDriverSocketResponseDeserializer
import io.cyborgsquirrel.jobs.streaming.util.ClientTimeSync
import io.cyborgsquirrel.lighting.effect_trigger.service.TriggerManager
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.model.RgbFrameData
import io.cyborgsquirrel.lighting.rendering.LightEffectRenderer
import io.cyborgsquirrel.util.time.TimeHelper
import io.micronaut.http.exceptions.ConnectionClosedException
import kotlinx.coroutines.*
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import kotlin.math.min

/**
 * Background job for streaming light effects to NightDriver clients
 *
 * Communicates with the NightDriver client with the following steps
 * 1. Checks if the NightDriver client and LED strips are configured.
 * If at any point one or both of these is deleted the job returns to this step.
 * 2. A TCP socket connection is established. Note: this is NOT a WebSocket.
 * 3. Effect rendering. The light effect RGB data is streamed to the NightDriver client. If the buffer is full the
 * job pauses to wait for the NightDriver client to render one or more frames.
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
    private val bufferTimeMillis = 500L
    private var lastSeenAt = 0L
    private var sleepMillis = 0L
    private var lastTimeSyncPerformedAt = 0L
    private val clientTimeSync = ClientTimeSync(timeHelper)
    private val clientTimeOffset: Long
        get() = clientTimeSync.clientTimeOffset

    // State/logic
    private var exponentialReconnectionBackoffValue = 1
    private val exponentialReconnectionBackoffValueMax = 8
    private val fps = 35
    private val millisPerFrame: Long
        get() = 1000L / fps
    private var shouldRun = true
    private var state = StreamingJobState.SetupIncomplete
    private var lastResponse: NightDriverSocketResponse? = null
    private var lastResponseReceivedAt = 0L

    fun getLatestResponse() = lastResponse

    override fun getCurrentState() = state

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

    private suspend fun processState() {
        try {
            when (state) {
                StreamingJobState.SetupIncomplete -> {
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
                                stripEntity.blendMode!!,
                                stripEntity.brightness!!,
                            )
                            state = StreamingJobState.Offline
                        } else {
                            delay(5000)
                        }
                    } else {
                        delay(5000)
                    }
                }

                StreamingJobState.ConnectedIdle -> {
                    // Clear lastResponse when a connection is made in case this is a re-connection to avoid using stale data
                    lastResponse = null
                    // Socket connection = response from Night Driver
                    lastResponseReceivedAt = timeHelper.millisSinceEpoch()
                    state = StreamingJobState.TimeSyncRequired
                }

                StreamingJobState.Offline -> {
                    logger.info("Client $clientEntity disconnected. Attempting to reconnect...")
                    setupSocket()
                }

                StreamingJobState.SettingsSync -> {
                    // Not supported by Night Driver - we should never end up in this state
                    throw Exception("Unexpected state! $state")
                }

                StreamingJobState.BufferFullWaiting -> {
                    delay(sleepMillis)
                    state = StreamingJobState.RenderingEffect
                }

                StreamingJobState.WaitingForConnection -> {
                    // Not supported by NightDriver - we should never end up in this state
                    throw Exception("Unexpected state! $state")
                }

                StreamingJobState.TimeSyncRequired -> {
                    val now = timeHelper.millisSinceEpoch()
                    timestampMillis = now + clientTimeOffset + bufferTimeMillis
                    state = StreamingJobState.RenderingEffect
                }

                StreamingJobState.RenderingEffect -> {
                    triggerManager.processTriggers()
                    val frameOptional = renderer.renderFrame(strip.getUuid(), 0)

                    if (frameOptional.isEmpty) {
                        // Sleep for the equivalent of 2 frames
                        sleepMillis = millisPerFrame * 2
                        state = StreamingJobState.BufferFullWaiting
                    } else {
                        val now = timeHelper.millisSinceEpoch()
                        timestampMillis += millisPerFrame
                        logger.debug("Frame timestamp {}", timeHelper.dateTimeFromMillis(timestampMillis))

                        // Assemble RGB data
                        val frame = frameOptional.get()
                        val rgbData = frame.frameData
                        val frameData = RgbFrameData(timestampMillis, rgbData)

                        // Serialize and send frame - options are not supported for NightDriver
                        val encodedFrame = serializer.encode(frameData, strip.getPin().toInt())
                        // Do time sync once every five minutes
                        val isTimeSyncFrame = lastTimeSyncPerformedAt + (1000 * 60 * 5) < now

                        if (isTimeSyncFrame) {
                            clientTimeSync.doTimeSync {
                                sendSocketFrame(encodedFrame)
                                if (lastResponse != null) {
                                    lastTimeSyncPerformedAt = now
                                    lastResponse!!.currentClockMillis()
                                } else {
                                    -1
                                }
                            }

                            state = StreamingJobState.TimeSyncRequired
                        } else {
                            sendSocketFrame(encodedFrame)

                            // Sleep for the duration of 40% of one frame to avoid overloading the Night Driver client
                            delay((millisPerFrame * 0.4).toLong())
                            if (socket?.isConnected == false) {
                                resetStateOnError()
                            } else if (lastResponseReceivedAt + 5000 < now) {
                                logger.info("Last response from $clientEntity was more than 5 seconds ago.")
                                resetStateOnError()
                                // Wait before reconnection. Don't wait more than 5 minutes.
                                delay(min(now - lastResponseReceivedAt, 5 * 60 * 1000))
                            } else if (lastResponse != null) {
                                updateLastSeenAt(now)
                                // Reset the exponential backoff value once we're both connected AND have received a response
                                exponentialReconnectionBackoffValue = 1
                                val millisOfFramesBuffered = lastResponse!!.bufferPosition * millisPerFrame
                                val clientBufferFull = lastResponse!!.bufferPosition == lastResponse!!.bufferSize
                                if (millisOfFramesBuffered > bufferTimeMillis || clientBufferFull) {
                                    logger.debug("Client buffer full, waiting.")
                                    sleepMillis = millisPerFrame / 2
                                    state = StreamingJobState.BufferFullWaiting
                                }
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

    private suspend fun sendSocketFrame(frame: ByteArray) {
        try {
            logger.debug("Sending frame...")
            withContext(Dispatchers.IO) {
                socket?.getOutputStream()?.write(frame)
                socket?.getOutputStream()?.flush()

                val availableBytes = socket?.getInputStream()?.available()?.toLong()
                if (availableBytes != null && availableBytes > 0L) {
                    val bytes = ByteArray(NightDriverSocketResponse.SIZE_IN_BYTES)
                    socket?.getInputStream()?.read(bytes)
                    lastResponse = deserializer.deserialize(bytes)
                    lastResponseReceivedAt = timeHelper.millisSinceEpoch()
                    logger.debug("Night Driver client {} status {}", clientEntity, lastResponse)
                    logger.debug("Client clock {}", timeHelper.dateTimeFromMillis(lastResponse!!.currentClockMillis()))
                }
            }
        } catch (sockEx: SocketException) {
            resetStateOnError()
        }
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
            logger.info("Connected to $clientEntity")
            state = StreamingJobState.ConnectedIdle
        } else {
            resetStateOnError()
        }
    }

    private fun resetStateOnError() {
        if (state != StreamingJobState.SetupIncomplete) {
            state = StreamingJobState.Offline
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