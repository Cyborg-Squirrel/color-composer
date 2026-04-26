package io.cyborgsquirrel.home.services

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.home.responses.HomeResponse
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.lighting.effect_palette.repository.H2LightEffectPaletteRepository
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.service.EffectApiService
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import jakarta.inject.Singleton

@Singleton
class HomeApiService(
    private val clientRepository: H2LedStripClientRepository,
    private val stripRepository: H2LedStripRepository,
    private val effectRepository: H2LightEffectRepository,
    private val paletteRepository: H2LightEffectPaletteRepository,
    private val effectApiService: EffectApiService
) {
    fun getHome(): HomeResponse {
        val activeEffects = effectApiService.getAllEffectsWithStatus(LightEffectStatus.activeStatuses())

        return HomeResponse(
            clients = clientRepository.count().toInt(),
            strips = stripRepository.count().toInt(),
            effects = effectRepository.count().toInt(),
            palettes = paletteRepository.count().toInt(),
            activeEffects = activeEffects.effects,
        )
    }
}
