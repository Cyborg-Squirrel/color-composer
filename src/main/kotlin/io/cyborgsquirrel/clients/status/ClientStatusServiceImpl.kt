package io.cyborgsquirrel.clients.status

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientStatus
import io.cyborgsquirrel.jobs.streaming.StreamJobManager
import io.cyborgsquirrel.jobs.streaming.StreamingJobState
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import jakarta.inject.Singleton
import java.util.*

@Singleton
class ClientStatusServiceImpl(
    private val activeLightEffectRegistry: ActiveLightEffectRegistry,
    private val jobsManager: StreamJobManager
) : ClientStatusService {

    override fun getStatusForClient(clientEntity: LedStripClientEntity): Optional<ClientStatusInfo> {
        val clientJobState = jobsManager.getJobState(clientEntity)
        if (clientJobState != null) {
            return when (clientJobState) {
                StreamingJobState.SetupIncomplete -> Optional.of(ClientStatusInfo.inactive(ClientStatus.SetupIncomplete))
                StreamingJobState.WaitingForConnection -> Optional.of(ClientStatusInfo.inactive(ClientStatus.Offline))
                StreamingJobState.DisconnectedIdle -> Optional.of(ClientStatusInfo.inactive(ClientStatus.Offline))
                StreamingJobState.ConnectedIdle -> Optional.of(ClientStatusInfo.inactive(ClientStatus.Idle))
                else -> {
                    val strips = clientEntity.strips
                    var activeEffects = 0
                    for (strip in strips) {
                        val effectsForStrip = activeLightEffectRegistry.getAllEffectsForStrip(strip.uuid!!)
                            .filter { it.status == LightEffectStatus.Playing || it.status == LightEffectStatus.Paused }
                        activeEffects += effectsForStrip.size
                    }

                    if (activeEffects > 0) {
                        Optional.of(ClientStatusInfo(ClientStatus.Active, activeEffects))
                    } else {
                        Optional.of(ClientStatusInfo.inactive(ClientStatus.Idle))
                    }
                }
            }
        } else {
            return Optional.empty()
        }
    }
}