package io.cyborgsquirrel.clients.config.pi_client

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.model.ClientTime
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.uri.UriBuilder
import io.micronaut.serde.ObjectMapper
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class PiConfigClient(
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper,
) {

    suspend fun getConfigs(client: LedStripClientEntity): PiClientConfigList {
        val uri = getConfigurationUriBuilder(client).build()
        val response = withContext(Dispatchers.IO) {
            httpClient.toBlocking().retrieve(uri.toString())
        }
        val configList = objectMapper.readValue(response, PiClientConfigList::class.java)
        return configList
    }

    suspend fun addConfig(client: LedStripClientEntity, config: PiClientConfig) {
        val uri = getConfigurationUriBuilder(client).build()
        val request = HttpRequest.POST(uri.toString(), config)

        withContext(Dispatchers.IO) {
            httpClient.toBlocking().exchange(request, String::class.java)
        }
    }

    suspend fun updateConfig(client: LedStripClientEntity, config: PiClientConfig) {
        val uri = getConfigurationUriBuilder(client).build()
        val request = HttpRequest.DELETE<String>(uri.toString())

        withContext(Dispatchers.IO) {
            httpClient.toBlocking().exchange(request, String::class.java)
        }
    }

    suspend fun deleteConfig(client: LedStripClientEntity, config: PiClientConfig) {
        val uri = getConfigurationUriBuilder(client)
            .queryParam("uuid", config.uuid)
            .build()
        val request = HttpRequest.DELETE<String>(uri.toString())

        withContext(Dispatchers.IO) {
            httpClient.toBlocking().exchange(request, String::class.java)
        }
    }

    suspend fun getClientTime(client: LedStripClientEntity): ClientTime {
        val uri = UriBuilder.of(client.address)
            .port(client.apiPort!!)
            .path("time")
            .build()

        val response = withContext(Dispatchers.IO) {
            httpClient.toBlocking().retrieve(uri.toString())
        }
        val timeObj = objectMapper.readValue(response, ClientTime::class.java)
        return timeObj
    }

    private fun getConfigurationUriBuilder(client: LedStripClientEntity): UriBuilder {
        return UriBuilder.of(client.address)
            .port(client.apiPort!!)
            .path("configuration")
    }
}