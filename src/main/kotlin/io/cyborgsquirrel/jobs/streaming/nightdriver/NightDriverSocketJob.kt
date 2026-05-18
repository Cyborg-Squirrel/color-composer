package io.cyborgsquirrel.jobs.streaming.nightdriver

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.jobs.streaming.ClientStreamingJob
import io.cyborgsquirrel.jobs.streaming.model.NightDriverStreamingJobState
import io.cyborgsquirrel.jobs.streaming.model.StreamingJobStatus
import io.cyborgsquirrel.jobs.streaming.serialization.NightDriverFrameDataSerializer
import io.cyborgsquirrel.jobs.streaming.util.ClientTimeSync
import io.cyborgsquirrel.lighting.effect_trigger.service.TriggerManager
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.service.LightEffectRegistry
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.model.LedStripPoolModel
import io.cyborgsquirrel.lighting.model.RgbFrameData
import io.cyborgsquirrel.lighting.model.SingleLedStripModel
import io.cyborgsquirrel.lighting.rendering.LightEffectRenderer
import io.cyborgsquirrel.util.time.TimeHelper
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import reactor.core.Disposable
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.net.URI

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
    private val clientRepository: LedStripClientRepository,
    private val timeHelper: TimeHelper,
    private var clientEntity: LedStripClientEntity,
    private var activeLightEffectService: LightEffectRegistry,
) : ClientStreamingJob {

    // NightDriver TCP connection
    private var socket: Socket? = null
    private var effectsSubscription: Disposable? = null

    // Client data — written from both the coroutine and the onUpdate listener callback
    @Volatile
    private var strips: List<LedStripModel> = emptyList()

    // Serialization
    private val serializer = NightDriverFrameDataSerializer()

    // Time tracking
    private var timestampMillis = 0L
    private val bufferTimeMillis = 500L
    private var lastSeenAt = 0L
    private var sleepMillis = 0L
    private var lastTimeSyncPerformedAt = 0L
    private val clientTimeSync = ClientTimeSync(timeHelper)
    private val clientTimeOffset: Long
        get() = clientTimeSync.mostRecentClientTimeOffset

    // State/logic — status/lastResponse/lastResponseReceivedAt written from both coroutine and IO thread
    private var exponentialReconnectionBackoffValue = 1
    private val exponentialReconnectionBackoffValueMax = 8

    private val fps get() = clientEntity.fps
    private val millisPerFrame: Long
        get() = 1000L / fps
    private var shouldRun = true

    @Volatile
    private var status = StreamingJobStatus.SetupIncomplete

    @Volatile
    private var lastResponse: NightDriverSocketResponse? = null

    @Volatile
    private var lastResponseReceivedAt = 0L

    fun getLatestResponse() = lastResponse

    /**
     * Starts the job which will run in the background using a Kotlin Coroutine.
     * Returns the Job instance.
     */
    override fun start(scope: CoroutineScope): Job {
        effectsSubscription = activeLightEffectService.updates.subscribe { onUpdate(it) }
        return scope.launch {
            logger.info("Start")
            while (isActive && shouldRun) {
                processState()
            }
            logger.info("Done")
        }
    }

    override fun getCurrentState() = NightDriverStreamingJobState(status, lastResponse)

    private suspend fun processState() {
        try {
            when (status) {
                StreamingJobStatus.SetupIncomplete -> {
                    val clientOptional = clientRepository.findByUuid(clientEntity.uuid)
                    if (clientOptional.isPresent) {
                        clientEntity = clientOptional.get()
                        if (clientEntity.strips.isNotEmpty()) {
                            strips = activeLightEffectService.getEffectsForClient(clientEntity.uuid).map { it.strip }
                            status = StreamingJobStatus.Offline
                        } else {
                            delay(5000)
                        }
                    } else {
                        dispose()
                    }
                }

                StreamingJobStatus.ConnectedIdle -> {
                    // Clear lastResponse when a connection is made in case this is a re-connection to avoid using stale data
                    lastResponse = null
                    // Socket connection = response from Night Driver
                    lastResponseReceivedAt = timeHelper.millisSinceEpoch()
                    status = StreamingJobStatus.RenderingEffect
                }

                StreamingJobStatus.Offline -> {
                    logger.info("Client $clientEntity disconnected. Attempting to reconnect...")
                    delay(1000)
                    setupSocket()
                }

                StreamingJobStatus.SettingsSync -> {
                    // Not supported by Night Driver - we should never end up in this state
                    throw Exception("Unexpected state! $status")
                }

                StreamingJobStatus.BufferFullWaiting -> {
                    delay(sleepMillis)
                    status = StreamingJobStatus.RenderingEffect
                }

                StreamingJobStatus.WaitingForConnection -> {
                    // Not supported by NightDriver - we should never end up in this state
                    throw Exception("Unexpected state! $status")
                }

                StreamingJobStatus.TimeSyncRequired -> {
                    // Not supported by NightDriver - we should never end up in this state
                    throw Exception("Unexpected state! $status")
                }

                StreamingJobStatus.RenderingEffect -> {
                    triggerManager.processTriggers()
                    val frameList = renderer.renderFrames(strips, clientEntity.uuid)

                    if (frameList.isEmpty()) {
                        // Sleep for the equivalent of 2 frames
                        sleepMillis = millisPerFrame * 2
                        status = StreamingJobStatus.BufferFullWaiting
                    } else {
                        val now = timeHelper.millisSinceEpoch()
                        timestampMillis = now + clientTimeOffset + millisPerFrame
                        logger.debug("Frame timestamp {}", timeHelper.dateTimeFromMillis(timestampMillis))

                        // Assemble RGB data
                        val encodedFrames = mutableListOf<ByteArray>()
                        for (frame in frameList) {
                            val rgbData = frame.frameData
                            val frameData = RgbFrameData(timestampMillis, rgbData)

                            // Serialize and send frame - options are not supported for NightDriver
                            val encodedFrame = serializer.encode(frameData, frame.strip.pin.toInt())
                            encodedFrames.add(encodedFrame)
                        }

                        for (encodedFrame in encodedFrames) {
                            // Sync time once every 5 minutes
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
                            } else {
                                sendSocketFrame(encodedFrame)
                            }
                        }

                        // Sleep for the duration of half of one frame to avoid overloading the Night Driver client
                        delay(millisPerFrame / 2)
                        if (socket?.isConnected == false) {
                            resetStateOnError()
                        } else if (lastResponseReceivedAt + 5000 < now) {
                            logger.info("Last response from $clientEntity was more than 5 seconds ago.")
                            resetStateOnError()
                        } else if (lastResponse != null) {
                            updateLastSeenAt(now)
                            // Reset the exponential backoff value once we're both connected AND have received a response
                            exponentialReconnectionBackoffValue = 1
                            val millisOfFramesBuffered = lastResponse!!.bufferPosition * millisPerFrame
                            val clientBufferFull =
                                lastResponse!!.bufferPosition + strips.size >= lastResponse!!.bufferSize
                            if (millisOfFramesBuffered > bufferTimeMillis || clientBufferFull) {
                                logger.debug("Client buffer full, waiting.")
                                sleepMillis = millisPerFrame
                                status = StreamingJobStatus.BufferFullWaiting
                            } else if (lastResponse!!.frameDrawing > fps) {
                                sleepMillis = millisPerFrame / 2
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

                val inputStream = socket?.getInputStream()
                if (inputStream != null) {
                    val bytes = ByteArray(NightDriverSocketResponse.SIZE_IN_BYTES)
                    var offset = 0
                    while (offset < bytes.size) {
                        val read = inputStream.read(bytes, offset, bytes.size - offset)
                        if (read == -1) break
                        offset += read
                    }
                    if (offset == bytes.size) {
                        lastResponse = bytes.toNightDriverSocketResponse(timeHelper)
                        lastResponseReceivedAt = timeHelper.millisSinceEpoch()
                        logger.debug("Night Driver client {} status {}", clientEntity, lastResponse)
                        logger.debug(
                            "Client clock {}",
                            timeHelper.dateTimeFromMillis(lastResponse!!.currentClockMillis())
                        )
                    }
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
                lastResponse?.let { ce.firmwareVersion = it.flashVersion.toString() }
                clientEntity = clientRepository.update(ce)
            }
        }
    }

    private suspend fun setupSocket() {
        val host = parseHost(clientEntity.address)

        withTimeout(10_000L) {
            withContext(Dispatchers.IO) {
                try {
                    socket?.close()
                } catch (_: Exception) {
                }

                socket = Socket()
                socket?.connect(InetSocketAddress(host, clientEntity.wsPort), 5000)
            }
        }

        if (socket?.isConnected == true) {
            logger.info("Connected to $clientEntity")
            status = StreamingJobStatus.ConnectedIdle
        } else {
            resetStateOnError()
        }
    }

    private fun resetStateOnError() {
        if (status != StreamingJobStatus.SetupIncomplete) {
            status = StreamingJobStatus.Offline
        }
    }

    override fun dispose() {
        effectsSubscription?.dispose()
        shouldRun = false
        socket?.close()
    }

    internal fun onUpdate(newEffects: List<ActiveLightEffect>) {
        val matchingStrips = newEffects.filter {
            val strip = it.strip
            val clientUuid = clientEntity.uuid
            when (strip) {
                is SingleLedStripModel -> strip.clientUuid == clientUuid
                is LedStripPoolModel -> strip.clientUuids().contains(clientUuid)
            }
        }.map { it.strip }

        if (strips != matchingStrips) {
            // NightDriver has no settings-sync step, so the new strip list takes effect on the next rendered frame.
            strips = matchingStrips
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NightDriverSocketJob::class.java)

        fun parseHost(address: String): String {
            return try {
                URI(address).host ?: address
            } catch (_: Exception) {
                address
            }
        }
    }
}
