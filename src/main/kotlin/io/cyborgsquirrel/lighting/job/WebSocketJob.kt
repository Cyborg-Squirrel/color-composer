package io.cyborgsquirrel.lighting.job

import io.cyborgsquirrel.lighting.client.LedStripWebSocketClient
import io.cyborgsquirrel.lighting.config.WebSocketJobConfig
import io.cyborgsquirrel.lighting.effect_trigger.enums.TriggerType
import io.cyborgsquirrel.lighting.effect_trigger.settings.TimeTriggerSettings
import io.cyborgsquirrel.lighting.effect_trigger.triggers.TimeTrigger
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.AnimatedSpectrumLightEffect
import io.cyborgsquirrel.lighting.effects.NightriderLightEffect
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.enums.ReflectionType
import io.cyborgsquirrel.lighting.rendering.LightEffectRenderer
import io.cyborgsquirrel.lighting.rendering.filters.BrightnessFilter
import io.cyborgsquirrel.lighting.rendering.filters.ReflectionFilter
import io.cyborgsquirrel.lighting.rendering.filters.ReverseFilter
import io.cyborgsquirrel.model.color.RgbFrameData
import io.cyborgsquirrel.model.color.RgbColor
import io.cyborgsquirrel.lighting.serialization.RgbFrameDataSerializer
import io.cyborgsquirrel.model.strip.LedStripModel
import io.cyborgsquirrel.sunrise_sunset.time.TimeHelperImpl
import io.micronaut.http.uri.UriBuilder
import io.micronaut.scheduling.TaskScheduler
import io.micronaut.websocket.WebSocketClient
import jakarta.inject.Singleton
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/**
 * Background job for streaming RgbFrameData with clients
 */
@Singleton
class WebSocketJob(
    private val taskScheduler: TaskScheduler,
    private val webSocketClient: WebSocketClient,
    private val renderer: LightEffectRenderer
) : Runnable {

    private val configList = mutableListOf<WebSocketJobConfig>()
    private val serializer = RgbFrameDataSerializer()
    private var client: LedStripWebSocketClient? = null

    override fun run() {
        logger.info("Start")
        try {
            setupWebSocket()
            val timestamp = Timestamp.from(Instant.now().plusMillis(50))
            var timestampMillis = timestamp.time
            val strip = LedStripModel("Living Room", UUID.randomUUID().toString(), 60)
            val effect = NightriderLightEffect(60)
//            val effect = AnimatedSpectrumLightEffect(60, 9)
            val filters = listOf(BrightnessFilter(0.25f), ReverseFilter(), ReflectionFilter(ReflectionType.HighToLow))
            val activeEffect = ActiveLightEffect(
                UUID.randomUUID().toString(), 1, LightEffectStatus.Active, effect, strip, filters
            )
            renderer.addOrUpdateEffect(activeEffect)
            val triggerSettings =
                TimeTriggerSettings(LocalTime.now().plusSeconds(90), null, null, TriggerType.StopEffect)
            val timeHelper = TimeHelperImpl()
            val trigger = TimeTrigger(timeHelper, triggerSettings)

            while (trigger.lastActivation().isEmpty) {
                val iterationsString = effect.getIterations().toString()
                val numberLength = min(iterationsString.length, 2)
                val startIndex = max(iterationsString.length - numberLength, 0)
                val iterationTwoDigits = iterationsString.substring(startIndex, startIndex + numberLength).toInt()
                when (iterationTwoDigits) {
                    0 -> {
                        renderer.addOrUpdateEffect(activeEffect.copy(filters = filters))
                    }

                    25 -> {
                        renderer.addOrUpdateEffect(activeEffect.copy(filters = filters.map {
                            if (it is ReflectionFilter) ReflectionFilter(
                                ReflectionType.LowToHigh
                            ) else it
                        }))
                    }

                    50 -> {
                        renderer.addOrUpdateEffect(activeEffect.copy(filters = filters.filter { it is ReverseFilter || it is BrightnessFilter }))
                    }

                    75 -> {
                        renderer.addOrUpdateEffect(activeEffect.copy(filters = filters.filterIsInstance<BrightnessFilter>()))
                    }
                }

                timestampMillis += 1000 / 35
                val rgbData = renderer.renderFrame(strip.getUuid(), 0).frameData
                val frameData = RgbFrameData(timestampMillis, rgbData)
                val frame = serializer.encode(frameData)
                client?.send(frame)?.get(1, TimeUnit.SECONDS)

                // Slow down to ensure we only buffer one second's worth of frames.
                // Prevent overloading of the client and improve responsiveness to a request to start/stop the effect.
                val nowPlusOneSecond = Timestamp.from(Instant.now().plusSeconds(1)).time
                if (nowPlusOneSecond < timestampMillis) {
                    val diff = timestampMillis - nowPlusOneSecond
                    sleep(diff)
                }
            }

            val rgbData = mutableListOf<RgbColor>()
            for (i in 0..59) {
                rgbData.add(RgbColor.Blank)
            }

            timestampMillis += 5
            val frameData = RgbFrameData(timestampMillis, rgbData)
            val frame = serializer.encode(frameData)
            client?.send(frame)?.get(1, TimeUnit.SECONDS)
            sleep(10)

            client?.close()
            logger.info("Done")
        } catch (ex: Exception) {
            ex.printStackTrace()
            logger.error(ex.toString())
        }
    }

    private fun setupWebSocket() {
        val uri = UriBuilder.of("ws://192.168.1.10")
            .port(8765)
            .path("test2")
            .build()
//        val uri = UriBuilder.of("${WebSocketClient.SCHEME_WS}://${config.ipAddress}")
//            .port(config.port)
//            .path(config.lightStripIds.first())
//            .build()
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