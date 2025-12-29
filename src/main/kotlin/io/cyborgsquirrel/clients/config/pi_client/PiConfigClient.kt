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

    suspend fun getStripConfigs(client: LedStripClientEntity): PiClientStripsConfigList {
        val uri = getStripsConfigurationUriBuilder(client).build()
        val response = withContext(Dispatchers.IO) {
            httpClient.toBlocking().retrieve(uri.toString())
        }
        val configList = objectMapper.readValue(response, PiClientStripsConfigList::class.java)
        return configList
    }

    suspend fun addStripConfig(client: LedStripClientEntity, config: PiClientStripConfig) {
        val uri = getStripsConfigurationUriBuilder(client).build()
        val request = HttpRequest.POST(uri.toString(), config)

        withContext(Dispatchers.IO) {
            httpClient.toBlocking().exchange(request, String::class.java)
        }
    }

    suspend fun updateStripConfig(client: LedStripClientEntity, config: PiClientStripConfig) {
        val uri = getStripsConfigurationUriBuilder(client).build()
        val request = HttpRequest.PATCH(uri.toString(), config)

        withContext(Dispatchers.IO) {
            httpClient.toBlocking().exchange(request, String::class.java)
        }
    }

    suspend fun deleteStripConfig(client: LedStripClientEntity, config: PiClientStripConfig) {
        val uri = getStripsConfigurationUriBuilder(client)
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

    suspend fun getClientSettings(client: LedStripClientEntity): PiClientSettings {
        val uri = getGlobalSettingsUriBuilder(client).build()
        val response = withContext(Dispatchers.IO) {
            httpClient.toBlocking().retrieve(uri.toString())
        }
        val configList = objectMapper.readValue(response, PiClientSettings::class.java)
        return configList
    }

    suspend fun updateClientSettings(client: LedStripClientEntity, settings: PiClientSettings) {
        val uri = getGlobalSettingsUriBuilder(client).build()
        val request = HttpRequest.PATCH(uri.toString(), settings)

        withContext(Dispatchers.IO) {
            httpClient.toBlocking().exchange(request, String::class.java)
        }
    }

    private fun getStripsConfigurationUriBuilder(client: LedStripClientEntity): UriBuilder {
        return UriBuilder.of(client.address)
            .port(client.apiPort!!)
            .path("strips-config")
    }

    private fun getGlobalSettingsUriBuilder(client: LedStripClientEntity): UriBuilder {
        return UriBuilder.of(client.address)
            .port(client.apiPort!!)
            .path("settings")
    }
}