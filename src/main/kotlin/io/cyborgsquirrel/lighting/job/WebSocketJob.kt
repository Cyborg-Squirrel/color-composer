package io.cyborgsquirrel.lighting.job

import io.cyborgsquirrel.client_config.client.ConfigClient
import io.cyborgsquirrel.entity.LedStripClientEntity
import io.cyborgsquirrel.lighting.client.LedStripWebSocketClient
import io.cyborgsquirrel.lighting.config.WebSocketJobConfig
import io.cyborgsquirrel.lighting.effect_trigger.TriggerManager
import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.cyborgsquirrel.lighting.effect_trigger.settings.TimeTriggerSettings
import io.cyborgsquirrel.lighting.effect_trigger.triggers.TimeTrigger
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.BouncingBallLightEffect
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.effects.settings.BouncingBallEffectSettings
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.enums.ReflectionType
import io.cyborgsquirrel.lighting.rendering.LightEffectRenderer
import io.cyborgsquirrel.lighting.rendering.filters.BrightnessFadeFilter
import io.cyborgsquirrel.lighting.rendering.filters.ReflectionFilter
import io.cyborgsquirrel.lighting.rendering.filters.ReverseFilter
import io.cyborgsquirrel.lighting.rendering.limits.PowerLimiterService
import io.cyborgsquirrel.lighting.serialization.RgbFrameDataSerializer
import io.cyborgsquirrel.model.color.RgbColor
import io.cyborgsquirrel.model.color.RgbFrameData
import io.cyborgsquirrel.model.strip.LedStripModel
import io.cyborgsquirrel.util.time.TimeHelper
import io.micronaut.http.uri.UriBuilder
import io.micronaut.websocket.WebSocketClient
import jakarta.inject.Singleton
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Background job for streaming RgbFrameData with clients
 */
@Singleton
class WebSocketJob(
    private val webSocketClient: WebSocketClient,
    private val renderer: LightEffectRenderer,
    private val triggerManager: TriggerManager,
    private val effectRepository: ActiveLightEffectRegistry,
    private val timeHelper: TimeHelper,
    private val powerLimiterService: PowerLimiterService,
    private val configClient: ConfigClient,
) : Runnable {

    private val configList = mutableListOf<WebSocketJobConfig>()
    private val serializer = RgbFrameDataSerializer()
    private var client: LedStripWebSocketClient? = null
    private val clientEntity =
        LedStripClientEntity(address = "http://192.168.1.10", name = "Pi Client", wsPort = 8765, apiPort = 8000)

    // Difference in millis between the client and server.
    // Negative values mean the client's clock is behind the server, positive values mean the client's clock is ahead.
    private var clientTimeOffset: Long = 0L
    private val fps = 35
    private val bufferTimeInSeconds = 1L
    private var lastTimeSyncPerformedAt = 0L

    override fun run() {
        logger.info("Start")
        try {
            setupWebSocket()
            syncClientTime()
            val strip = LedStripModel("Living Room", UUID.randomUUID().toString(), 60, 1)
            // Power supply is 4A
            powerLimiterService.setLimit(strip.getUuid(), 4000)

            val effects = listOf(
                BouncingBallLightEffect(
                    strip.getLength(),
                    timeHelper,
                    BouncingBallEffectSettings.default(strip.getLength())
                ),
                BouncingBallLightEffect(
                    strip.getLength(),
                    timeHelper,
                    BouncingBallEffectSettings.default(strip.getLength())
                        .copy(speed = 4.0, ballColor = RgbColor.Blue, startingHeight = 10.0, maxHeight = 50)
                ),
                BouncingBallLightEffect(
                    strip.getLength(),
                    timeHelper,
                    BouncingBallEffectSettings.default(strip.getLength())
                        .copy(speed = 4.0, ballColor = RgbColor.Green, startingHeight = 20.0, maxHeight = 40)
                ),
//                NightriderLightEffect(
//                    strip.getLength(),
//                    NightriderCometEffectSettings(RgbColor.Rainbow, 10, FadeCurve.Logarithmic, false)
//                ),
//                FlameLightEffect(60, FlameEffectSettings.default()),
//                SpectrumLightEffect(60, SpectrumEffectSettings.default(60).copy(colorPixelWidth = 9)),
//                NightriderLightEffect(
//                    60, NightriderColorFillEffectSettings(RgbColor.Rainbow.map { it.scale(0.2f) }, wrap = false)
//                )
            )

            val filters = listOf(
                BrightnessFadeFilter(0.1f, 1.0f, Duration.ofSeconds(30), timeHelper),
                ReverseFilter(),
                ReflectionFilter(ReflectionType.CopyOverCenter),
            )

            val activeEffects = effects.map { e ->
                ActiveLightEffect(
                    UUID.randomUUID().toString(), 1, true, LightEffectStatus.Created, e, strip, filters
                )
            }

            activeEffects.forEach {
                effectRepository.addOrUpdateEffect(it)
            }

            val triggerTime = LocalDateTime.now()
            val triggerSettings =
                TimeTriggerSettings(
                    triggerTime.toLocalTime(),
                    Duration.ofSeconds(60 * 1),
                    null,
                    TriggerType.StartEffect
                )
            val triggers = activeEffects.map { ae ->
                TimeTrigger(timeHelper, triggerSettings, UUID.randomUUID().toString(), ae.uuid)
            }
//            val triggerSettings =
//                SunriseSunsetTriggerSettings(
//                    SunriseSunsetOption.Sunrise,
//                    Duration.ofMinutes(10),
//                    null,
//                    TriggerType.StartEffect
//                )
//            val trigger = SunriseSunsetTrigger(
//                sunriseSunsetTimeRepository,
//                locationConfigRepository,
//                objectMapper,
//                timeHelper,
//                triggerSettings,
//                activeEffect.uuid
//            )
            triggers.forEach {
                triggerManager.addTrigger(it)
            }

            var lastFrameTimestamp = LocalDateTime.of(0, 1, 1, 0, 0)
            var timestampMillis = timeHelper.millisSinceEpoch() + (1000 / fps) + clientTimeOffset
            var isBlankFrame = false
            var hasHadNonBlankFrame = false
            var lastUpdateIteration = -1

            while (!isBlankFrame || !hasHadNonBlankFrame) {
                triggerManager.processTriggers()
                val iterationsString = effects.first().getIterations().toString()
                val numberLength = min(iterationsString.length, 2)
                val startIndex = max(iterationsString.length - numberLength, 0)
                val iterationTwoDigits = iterationsString.substring(startIndex, startIndex + numberLength).toInt()
                /*
                activeEffect = effectRepository.findEffectWithUuid(activeEffect.uuid).get()

                // Once we reach the specified iteration count update the effect in the repository
                // Set lastUpdateIteration to prevent duplicate updates in the registry
                if (lastUpdateIteration != iterationTwoDigits) {
                    when (iterationTwoDigits) {
                        0 -> {
                            lastUpdateIteration = iterationTwoDigits
                            effectRepository.addOrUpdateEffect(activeEffect.copy(filters = filters))
                        }

                        16 -> {
                            lastUpdateIteration = iterationTwoDigits
                            effectRepository.addOrUpdateEffect(activeEffect.copy(filters = filters.map {
                                if (it is ReflectionFilter) ReflectionFilter(
                                    ReflectionType.LowToHigh
                                ) else it
                            }))
                        }

                        33 -> {
                            lastUpdateIteration = iterationTwoDigits
                            effectRepository.addOrUpdateEffect(activeEffect.copy(filters = filters.filter { it is ReverseFilter || it is BrightnessFilter }))
                        }

                        66 -> {
                            lastUpdateIteration = iterationTwoDigits
                            effectRepository.addOrUpdateEffect(activeEffect.copy(filters = filters.filterIsInstance<BrightnessFilter>()))
                        }
                    }
                }
                 */

                val frame = renderer.renderFrame(strip.getUuid(), 0)
                isBlankFrame = frame.isEmpty || (frame.isPresent && frame.get().frameData.isEmpty())
                hasHadNonBlankFrame = hasHadNonBlankFrame || !isBlankFrame
                if (isBlankFrame) {
                    if (lastFrameTimestamp.plusMinutes(1).isBefore(LocalDateTime.now())) {
                        logger.info("Sending keep-alive frame")
                        val timestamp = timeHelper.millisSinceEpoch()
                        sendBlankFrame(timestamp)
                        lastFrameTimestamp = LocalDateTime.now()
                    } else {
                        sleep(250)
                    }
                } else {
                    val currentTimeAsMillis = timeHelper.millisSinceEpoch()
                    if (timestampMillis < currentTimeAsMillis + clientTimeOffset) {
                        logger.info("Frame timestamp is in the past. Jumping it forward to current time.")
                        syncClientTime()
                        timestampMillis = currentTimeAsMillis + clientTimeOffset
                    }
                    timestampMillis += 1000 / fps
                    val rgbData = frame.get().frameData
                    val frameData = RgbFrameData(timestampMillis, rgbData)
                    val encodedFrame = serializer.encode(frameData)
                    client?.send(encodedFrame)?.get(1, TimeUnit.SECONDS)
                    lastFrameTimestamp = LocalDateTime.now()

                    // Slow down to ensure we only buffer the specified amount of time into the future.
                    val nowPlusBufferSeconds = Timestamp.from(Instant.now().plusSeconds(bufferTimeInSeconds)).time
                    if (nowPlusBufferSeconds < timestampMillis) {
                        val diff = timestampMillis - nowPlusBufferSeconds
                        sleep(diff)
                    }
                }
            }

            sendBlankFrame(timestampMillis + 50)
            sleep(10)

            client?.close()
            logger.info("Done")
        } catch (ex: Exception) {
            ex.printStackTrace()
            logger.error(ex.toString())
        }
    }

    private fun sendBlankFrame(timestamp: Long) {
        val rgbData = mutableListOf<RgbColor>()
        for (i in 0..59) {
            rgbData.add(RgbColor.Blank)
        }

        val frameData = RgbFrameData(timestamp, rgbData)
        val frame = serializer.encode(frameData)
        client?.send(frame)?.get(1, TimeUnit.SECONDS)
    }

    private fun syncClientTime() {
        val timeSinceLastSync = timeHelper.millisSinceEpoch() - lastTimeSyncPerformedAt
        // Don't sync more often than every 60 seconds
        if (timeSinceLastSync > 1000 * 60) {
            val requestTimestamp = timeHelper.millisSinceEpoch()
            val clientTime = configClient.getClientTime(clientEntity)
            val responseTimestamp = timeHelper.millisSinceEpoch()

            // Assume request and response take the same amount of time for the network to transmit
            // Divide by 2 so we only count the transmission time going one way
            val requestResponseDuration = (responseTimestamp - requestTimestamp) / 2
            val adjustedClientTime = clientTime.millisSinceEpoch - requestResponseDuration
            val offset = adjustedClientTime - responseTimestamp

            // Ignore time offsets less than 10ms
            clientTimeOffset = if (abs(offset) > 10) {
                offset
            } else {
                0L
            }

            lastTimeSyncPerformedAt = responseTimestamp
            logger.info("Client time sync complete. Offset in millis: $clientTimeOffset")
        }
    }

    private fun setupWebSocket() {
        val uri = UriBuilder.of("ws://192.168.1.10")
            .port(8765)
            .path("test")
            .build()
        val future = CompletableFuture<LedStripWebSocketClient>()
        val clientPublisher = webSocketClient.connect(LedStripWebSocketClient::class.java, uri)
        clientPublisher.subscribe(object : Subscriber<LedStripWebSocketClient> {
            override fun onSubscribe(s: Subscription?) {
                s?.request(1)
            }

            override fun onError(t: Throwable?) {
                future.completeExceptionally(t)
            }

            override fun onComplete() {}

            override fun onNext(client: LedStripWebSocketClient?) {
                future.complete(client)
            }
        })

        client = future.get(5, TimeUnit.SECONDS)
    }

    fun dispose() {
        client?.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebSocketJob::class.java)
    }
}