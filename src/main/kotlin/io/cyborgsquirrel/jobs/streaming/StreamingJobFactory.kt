package io.cyborgsquirrel.jobs.streaming

import io.cyborgsquirrel.clients.config.pi_client.PiConfigClient
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.jobs.streaming.nightdriver.NightDriverSocketJob
import io.cyborgsquirrel.jobs.streaming.pi_client.PiClientWebSocketJob
import io.cyborgsquirrel.lighting.effect_trigger.service.TriggerManager
import io.cyborgsquirrel.lighting.effects.service.ActiveLightEffectService
import io.cyborgsquirrel.lighting.rendering.LightEffectRenderer
import io.cyborgsquirrel.util.time.TimeHelper
import io.micronaut.websocket.WebSocketClient
import jakarta.inject.Singleton

@Singleton
class StreamingJobFactory(
    private val webSocketClient: WebSocketClient,
    private val renderer: LightEffectRenderer,
    private val triggerManager: TriggerManager,
    private val clientRepository: H2LedStripClientRepository,
    private val timeHelper: TimeHelper,
    private val piConfigClient: PiConfigClient,
    private val activeLightEffectService: ActiveLightEffectService
) {

    fun createJob(client: LedStripClientEntity): ClientStreamingJob {
        return when (client.clientType) {
            ClientType.Pi -> {
                PiClientWebSocketJob(
                    webSocketClient,
                    renderer,
                    triggerManager,
                    clientRepository,
                    timeHelper,
                    piConfigClient,
                    client,
                    activeLightEffectService
                )
            }

            ClientType.NightDriver -> {
                NightDriverSocketJob(
                    renderer,
                    triggerManager,
                    clientRepository,
                    timeHelper,
                    client,
                    activeLightEffectService
                )
            }

            null -> throw Exception("No clientType specified!")
        }
    }
}