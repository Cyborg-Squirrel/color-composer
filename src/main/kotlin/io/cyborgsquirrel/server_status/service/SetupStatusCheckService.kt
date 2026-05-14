package io.cyborgsquirrel.server_status.service

import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.led_strips.repository.LedStripRepository
import io.cyborgsquirrel.lighting.effects.repository.LightEffectRepository
import io.cyborgsquirrel.server_status.responses.SetupStatus
import jakarta.inject.Singleton

@Singleton
class SetupStatusCheckService(
    private val clientRepository: LedStripClientRepository,
    private val stripRepository: LedStripRepository,
    private val effectRepository: LightEffectRepository,
) {

    fun getSetupStatus(): SetupStatus {
        val clients = clientRepository.count()
        if (clients == 0L) {
            return SetupStatus.NoClients
        }

        val strips = stripRepository.count()
        if (strips == 0L) {
            return SetupStatus.NoStrips
        }

        val effects = effectRepository.count()
        if (effects == 0L) {
            return SetupStatus.NoEffects
        }

        return SetupStatus.SetupComplete
    }
}