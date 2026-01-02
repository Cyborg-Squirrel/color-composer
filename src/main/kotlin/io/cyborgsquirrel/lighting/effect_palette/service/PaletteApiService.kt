package io.cyborgsquirrel.lighting.effect_palette.service

import io.cyborgsquirrel.lighting.effect_palette.EffectPaletteConstants
import io.cyborgsquirrel.lighting.effect_palette.entity.LightEffectPaletteEntity
import io.cyborgsquirrel.lighting.effect_palette.repository.H2LightEffectPaletteRepository
import io.cyborgsquirrel.lighting.effect_palette.requests.CreatePaletteRequest
import io.cyborgsquirrel.lighting.effect_palette.requests.UpdatePaletteRequest
import io.cyborgsquirrel.lighting.effect_palette.responses.GetAllPalettesResponse
import io.cyborgsquirrel.lighting.effect_palette.responses.GetPaletteResponse
import io.cyborgsquirrel.lighting.effect_palette.settings.*
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.service.CreateLightingService
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.micronaut.json.tree.JsonNode
import io.micronaut.serde.ObjectMapper
import jakarta.inject.Singleton
import java.util.*

@Singleton
class PaletteApiService(
    private val paletteRepository: H2LightEffectPaletteRepository,
    private val effectRepository: H2LightEffectRepository,
    private val lightingService: CreateLightingService,
    private val effectRegistry: ActiveLightEffectRegistry,
    private val objectMapper: ObjectMapper
) {

    fun getPalette(uuid: String): GetPaletteResponse {
        val paletteEntityOptional = paletteRepository.findByUuid(uuid)
        if (paletteEntityOptional.isPresent) {
            val paletteEntity = paletteEntityOptional.get()
            return GetPaletteResponse(
                paletteEntity.name!!,
                paletteEntity.uuid!!,
                paletteEntity.type!!,
                paletteEntity.settings!!
            )
        } else {
            throw ClientRequestException("Palette with uuid $uuid doesn't exist!")
        }
    }

    fun getAllPalettes(): GetAllPalettesResponse {
        val paletteEntities = paletteRepository.queryAll()
        val responsePaletteList = paletteEntities.map {
            GetPaletteResponse(it.name!!, it.uuid!!, it.type!!, it.settings!!)
        }

        return GetAllPalettesResponse(responsePaletteList)
    }

    fun createPalette(request: CreatePaletteRequest): String {
        val paletteSettingsValid = validatePalette(request.settings, request.paletteType)
        if (paletteSettingsValid) {
            var paletteEntity = LightEffectPaletteEntity(
                settings = request.settings,
                uuid = UUID.randomUUID().toString(),
                name = request.name,
                type = request.paletteType
            )

            paletteEntity = paletteRepository.save(paletteEntity)
            return paletteEntity.uuid!!
        } else {
            throw ClientRequestException("Palette settings are invalid")
        }
    }

    fun updatePalette(request: UpdatePaletteRequest, uuid: String) {
        val paletteEntityOptional = paletteRepository.findByUuid(uuid)
        if (paletteEntityOptional.isPresent) {
            var paletteEntity = paletteEntityOptional.get()
            if (request.settings != null) {
                val paletteSettingsValid = validatePalette(request.settings, paletteEntity.type!!)
                if (paletteSettingsValid) {
                    paletteEntity = paletteEntity.copy(settings = request.settings)
                } else {
                    throw ClientRequestException("Palette settings are invalid")
                }
            }

            if (request.name != null) {
                paletteEntity = paletteEntity.copy(name = request.name)
            }

            // Do all database updates first, then active effect updates
            paletteEntity = paletteRepository.update(paletteEntity)

            for (effectEntity in paletteEntity.effects) {
                val activeEffectOptional = effectRegistry.getEffectWithUuid(effectEntity.uuid!!)
                if (activeEffectOptional.isPresent) {
                    val activeEffect = activeEffectOptional.get()
                    val strip = lightingService.ledStripFromEffectEntity(effectEntity)
                    val palette = lightingService.createPalette(
                        paletteEntity.settings!!,
                        paletteEntity.type!!,
                        paletteEntity.uuid!!,
                        strip.length()
                    )
                    activeEffect.effect.updatePalette(palette)
                }
            }
        } else {
            throw ClientRequestException("Palette with uuid $uuid doesn't exist!")
        }
    }

    fun deletePalette(uuid: String) {
        val paletteEntityOptional = paletteRepository.findByUuid(uuid)
        if (paletteEntityOptional.isPresent) {
            val paletteEntity = paletteEntityOptional.get()
            val effectEntities = effectRepository.findByIdIn(paletteEntity.effects.map { it.id })
            for (effectEntity in effectEntities) {
                effectEntity.palette = null
                effectRepository.update(effectEntity)
            }

            paletteRepository.delete(paletteEntity)
        } else {
            throw ClientRequestException("Palette with uuid $uuid doesn't exist!")
        }
    }

    private fun validatePalette(settings: Map<String, Any>, paletteType: String): Boolean {
        when (paletteType) {
            EffectPaletteConstants.STATIC_COLOR_PALETTE -> {
                objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    StaticPaletteSettings::class.java
                )
            }

            EffectPaletteConstants.GRADIENT_COLOR_PALETTE_NAME -> {
                objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    GradientPaletteSettings::class.java
                )
            }

            EffectPaletteConstants.CHANGING_COLOR_GRADIENT_PALETTE_NAME -> {
                objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    ChangingGradientPaletteSettings::class.java
                )
            }

            EffectPaletteConstants.CHANGING_COLOR_STATIC_PALETTE_NAME -> {
                objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    ChangingStaticPaletteSettings::class.java
                )
            }

            EffectPaletteConstants.TIME_OF_DAY_COLOR_PALETTE -> {
                objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    TimeOfDayPaletteSettings::class.java
                )
            }

            EffectPaletteConstants.LOCAL_TIME_COLOR_PALETTE -> {
                objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    LocalTimePaletteSettings::class.java
                )
            }

            else -> {
                return false
            }
        }

        return true
    }
}