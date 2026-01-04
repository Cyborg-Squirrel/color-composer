package io.cyborgsquirrel.clients.status

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientStatus
import io.cyborgsquirrel.jobs.streaming.StreamJobManager
import io.cyborgsquirrel.jobs.streaming.StreamingJobState
import io.cyborgsquirrel.lighting.effects.service.ActiveLightEffectService
import io.cyborgsquirrel.lighting.enums.isActive
import jakarta.inject.Singleton
import java.util.*

@Singleton
class ClientStatusServiceImpl(
    private val activeLightEffectService: ActiveLightEffectService,
    private val jobsManager: StreamJobManager
) : ClientStatusService {

    override fun getStatusForClient(clientEntity: LedStripClientEntity): Optional<ClientStatusInfo> {
        val clientJobState = jobsManager.getJobState(clientEntity)
        if (clientJobState != null) {
            return when (clientJobState) {
                StreamingJobState.SetupIncomplete -> Optional.of(ClientStatusInfo.inactive(ClientStatus.SetupIncomplete))
                StreamingJobState.WaitingForConnection -> Optional.of(ClientStatusInfo.inactive(ClientStatus.Offline))
                StreamingJobState.Offline -> Optional.of(ClientStatusInfo.inactive(ClientStatus.Offline))
                StreamingJobState.ConnectedIdle -> Optional.of(ClientStatusInfo.inactive(ClientStatus.Idle))
                else -> {
                    val strips = clientEntity.strips
                    var activeEffects = 0
                    for (strip in strips) {
                        val effectsForStrip = activeLightEffectService.getAllEffectsForStrip(strip.uuid!!)
                            .filter { it.status.isActive() }
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