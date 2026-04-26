package io.cyborgsquirrel.home.services

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.home.responses.ActiveEffectResponse
import io.cyborgsquirrel.home.responses.HomeResponse
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.lighting.effect_palette.repository.H2LightEffectPaletteRepository
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.service.ActiveLightEffectService
import io.cyborgsquirrel.lighting.enums.isActive
import jakarta.inject.Singleton

@Singleton
class HomeApiService(
    private val clientRepository: H2LedStripClientRepository,
    private val stripRepository: H2LedStripRepository,
    private val effectRepository: H2LightEffectRepository,
    private val paletteRepository: H2LightEffectPaletteRepository,
    private val activeLightEffectService: ActiveLightEffectService,
) {
    fun getHome(): HomeResponse {
        val activeEffects = activeLightEffectService.getAllEffects()
            .filter { it.status.isActive() }
            .map { ActiveEffectResponse(uuid = it.effectUuid, status = it.status, stripUuid = it.strip.uuid) }

        return HomeResponse(
            clients = clientRepository.count().toInt(),
            strips = stripRepository.count().toInt(),
            effects = effectRepository.count().toInt(),
            palettes = paletteRepository.count().toInt(),
            activeEffects = activeEffects,
        )
    }
}
