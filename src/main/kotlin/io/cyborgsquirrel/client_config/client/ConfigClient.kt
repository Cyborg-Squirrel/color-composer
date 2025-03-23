package io.cyborgsquirrel.client_config.client

import io.cyborgsquirrel.client_config.model.ClientTime
import io.cyborgsquirrel.entity.LedStripClientEntity
import io.cyborgsquirrel.util.time.TimeHelper
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.uri.UriBuilder
import io.micronaut.serde.ObjectMapper
import jakarta.inject.Singleton
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@Singleton
class ConfigClient(
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper,
    private val timeHelper: TimeHelper,
) {

    fun getConfig(client: LedStripClientEntity) {
        val uri = UriBuilder.of(client.address)
            .port(client.apiPort!!)
            .path("configuration")
            .build()
        httpClient.retrieve(uri.toString())
    }

    fun setConfig(client: LedStripClientEntity) {
        val uri = UriBuilder.of(client.address)
            .port(client.apiPort!!)
            .path("configuration")
            .build()
        val requestMap = mapOf("a" to "b")
        val requestJson = objectMapper.writeValueAsString(requestMap)
        val request = HttpRequest.POST(uri.toString(), requestJson)

        httpClient.exchange(request)
    }

    fun getClientTime(client: LedStripClientEntity): ClientTime {
        val uri = UriBuilder.of(client.address)
            .port(client.apiPort!!)
            .path("time")
            .build()
        val pub = httpClient.retrieve(uri.toString())
        val future = CompletableFuture<ClientTime>()
        pub.subscribe(object : Subscriber<String> {
            override fun onSubscribe(s: Subscription?) {
                s?.request(1)
            }

            override fun onError(t: Throwable?) {
                future.completeExceptionally(t)
            }

            override fun onComplete() {}

            override fun onNext(response: String?) {
                val timeObj = objectMapper.readValue(response, ClientTime::class.java)
                future.complete(timeObj)
            }
        })

        val clientTime = future.get(3000, TimeUnit.SECONDS)
        return clientTime
    }
}