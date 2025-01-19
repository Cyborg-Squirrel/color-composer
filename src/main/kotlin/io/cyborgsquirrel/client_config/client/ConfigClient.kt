package io.cyborgsquirrel.client_config.client

import io.cyborgsquirrel.entity.LedStripClientEntity
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.uri.UriBuilder
import io.micronaut.serde.ObjectMapper
import jakarta.inject.Singleton

@Singleton
class ConfigClient(private val httpClient: HttpClient, private val objectMapper: ObjectMapper) {

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
}