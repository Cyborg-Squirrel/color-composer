package io.cyborgsquirrel.home.services

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.clients.services.LedClientApiService
import io.cyborgsquirrel.home.responses.HomeResponse
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.led_strips.services.LedStripApiService
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
    private val effectApiService: EffectApiService,
    private val clientApiService: LedClientApiService,
    private val stripsApiService: LedStripApiService,
) {
    fun getHome(): HomeResponse {
        val clientEntities = clientRepository.queryAll()
        val clientResponses = clientEntities.map {
            clientApiService.mapClientEntityToResponse(it)
        }
        val stripEntities = clientEntities.flatMap { it.strips }.distinctBy { it.uuid }
        val stripResponses = stripEntities.map {
            stripsApiService.mapStripEntityToResponse(it)
        }
        val activeEffectEntities = effectRepository.findByStatusIn(LightEffectStatus.activeStatuses())
        val activeEffectResponses = activeEffectEntities.mapNotNull { e -> effectApiService.getEffectResponseForEffect(e) }

        return HomeResponse(
            totalClients = clientEntities.size,
            totalStrips = stripRepository.count().toInt(),
            totalEffects = effectRepository.count().toInt(),
            totalPalettes = paletteRepository.count().toInt(),
            activeEffects = activeEffectResponses,
            strips = stripResponses,
            clients = clientResponses,
        )
    }
}
