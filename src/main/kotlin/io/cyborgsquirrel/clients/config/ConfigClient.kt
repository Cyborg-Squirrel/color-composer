package io.cyborgsquirrel.clients.config

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.model.ClientTime
import io.cyborgsquirrel.util.time.TimeHelper
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.uri.UriBuilder
import io.micronaut.serde.ObjectMapper
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ConfigClient(
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper,
    private val timeHelper: TimeHelper,
) {

    suspend fun getConfigs(client: LedStripClientEntity): PiClientConfigList {
        val uri = UriBuilder.of(client.address)
            .port(client.apiPort!!)
            .path("configuration")
            .build()
        val response = withContext(Dispatchers.IO) {
            httpClient.toBlocking().retrieve(uri.toString())
        }
        val configList = objectMapper.readValue(response, PiClientConfigList::class.java)
        return configList
    }

    suspend fun setConfigs(client: LedStripClientEntity, configList: PiClientConfigList) {
        val uri = UriBuilder.of(client.address)
            .port(client.apiPort!!)
            .path("configuration")
            .build()
        val request = HttpRequest.POST(uri.toString(), configList)

        withContext(Dispatchers.IO) {
            httpClient.toBlocking().exchange(request, PiClientConfig::class.java)
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
}