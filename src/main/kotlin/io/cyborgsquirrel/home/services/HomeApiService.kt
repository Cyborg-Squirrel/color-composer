package io.cyborgsquirrel.home.services

import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.clients.shared.ClientResponseAssembler
import io.cyborgsquirrel.home.responses.HomeResponse
import io.cyborgsquirrel.led_strips.repository.LedStripRepository
import io.cyborgsquirrel.led_strips.services.LedStripApiService
import io.cyborgsquirrel.lighting.effect_palette.repository.LightEffectPaletteRepository
import io.cyborgsquirrel.lighting.effects.repository.LightEffectRepository
import io.cyborgsquirrel.lighting.effects.service.EffectApiService
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import jakarta.inject.Singleton

@Singleton
class HomeApiService(
    private val clientRepository: LedStripClientRepository,
    private val stripRepository: LedStripRepository,
    private val effectRepository: LightEffectRepository,
    private val paletteRepository: LightEffectPaletteRepository,
    private val effectApiService: EffectApiService,
    private val clientResponseAssembler: ClientResponseAssembler,
    private val stripsApiService: LedStripApiService,
) {
    fun getHome(): HomeResponse {
        val clientEntities = clientRepository.queryAll()
        val clientResponses = clientEntities.map {
            clientResponseAssembler.mapClientEntityToResponse(it)
        }
        val stripEntities = clientEntities.flatMap { it.strips }.distinctBy { it.uuid }
        val stripResponses = stripEntities.map {
            stripsApiService.mapStripEntityToResponse(it)
        }
        val activeEffectEntities = effectRepository.findByStatusIn(LightEffectStatus.inUseStatuses())
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
