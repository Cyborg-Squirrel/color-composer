package io.cyborgsquirrel.lighting.job

import io.cyborgsquirrel.lighting.client.LedStripWebSocketClient
import io.cyborgsquirrel.lighting.config.WebSocketJobConfig
import io.cyborgsquirrel.lighting.effects.AnimatedSpectrumLightEffect
import io.cyborgsquirrel.lighting.effects.NightriderLightEffect
import io.cyborgsquirrel.model.color.RgbFrameData
import io.cyborgsquirrel.model.color.RgbColor
import io.cyborgsquirrel.serialization.RgbFrameDataSerializer
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Background job for streaming RgbFrameData with clients
 */
@Singleton
class WebSocketJob(private val taskScheduler: TaskScheduler, private val webSocketClient: WebSocketClient) : Runnable {

    private val configList = mutableListOf<WebSocketJobConfig>()
    private val serializer = RgbFrameDataSerializer()
    private var client: LedStripWebSocketClient? = null

    override fun run() {
        logger.info("Start")
        try {
            setupWebSocket()
            val timestamp = Timestamp.from(Instant.now().plusMillis(250))
            var timestampMillis = timestamp.time
//            val effect = NightriderLightEffect(
//                60,
//                mutableListOf(
//                    RgbColor.Red,
//                    RgbColor.Orange,
//                    RgbColor.Yellow,
//                    RgbColor.Green,
//                    RgbColor.Blue,
//                    RgbColor.Purple,
//                ),
//            )
//            effect.setBrightness(0.2f)
            val effect = AnimatedSpectrumLightEffect(60, 9, listOf())
            effect.setBrightness(0.334f)

            while (effect.getIterations() < 20) {
                timestampMillis += 1000 / 15
                val rgbData = effect.getNextStep()
                val frameData = RgbFrameData(timestampMillis, rgbData)
                val frame = serializer.encode(frameData)
                client?.send(frame)?.get(1, TimeUnit.SECONDS)
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