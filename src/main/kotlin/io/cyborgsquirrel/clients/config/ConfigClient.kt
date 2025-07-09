package io.cyborgsquirrel.clients.config

import io.cyborgsquirrel.clients.model.ClientTime
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
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
        // TODO: serialization of response to object
        httpClient.retrieve(uri.toString())
    }

    fun setConfig(client: LedStripClientEntity) {
        val uri = UriBuilder.of(client.address)
            .port(client.apiPort!!)
            .path("configuration")
            .build()
        // Placeholder
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

        val response = httpClient.toBlocking().retrieve(uri.toString())
        val timeObj = objectMapper.readValue(response, ClientTime::class.java)
        return timeObj
    }
}