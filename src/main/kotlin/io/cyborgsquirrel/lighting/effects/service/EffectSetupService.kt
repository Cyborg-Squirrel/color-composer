package io.cyborgsquirrel.lighting.effects.service

import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.responses.GetEffectResponse
import io.cyborgsquirrel.lighting.effects.responses.GetEffectsResponse
import io.cyborgsquirrel.lighting.effects.settings.*
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.micronaut.http.HttpResponse
import io.micronaut.json.tree.JsonNode
import io.micronaut.serde.ObjectMapper
import jakarta.inject.Singleton
import java.util.*

@Singleton
class EffectSetupService(
    private val objectMapper: ObjectMapper,
    private val stripRepository: H2LedStripRepository,
    private val effectRepository: H2LightEffectRepository
) {

    fun createEffect(request: CreateEffectRequest): String {
        val settingsObj: Any = when (request.effectType) {
            LightEffectConstants.SPECTRUM_NAME -> objectMapper.readValueFromTree(
                JsonNode.from(request.settings),
                SpectrumEffectSettings::class.java
            )

            LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME -> objectMapper.readValueFromTree(
                JsonNode.from(request.settings),
                NightriderColorFillEffectSettings::class.java
            )

            LightEffectConstants.NIGHTRIDER_COMET_NAME -> objectMapper.readValueFromTree(
                JsonNode.from(request.settings),
                NightriderCometEffectSettings::class.java
            )

            LightEffectConstants.FLAME_EFFECT_NAME -> objectMapper.readValueFromTree(
                JsonNode.from(request.settings),
                FlameEffectSettings::class.java
            )

            LightEffectConstants.BOUNCING_BALL_NAME -> objectMapper.readValueFromTree(
                JsonNode.from(request.settings),
                BouncingBallEffectSettings::class.java
            )

            LightEffectConstants.WAVE_EFFECT_NAME -> TODO("Move wave effect constructor parameters to settings class")
            else -> throw ClientRequestException("No effect matching type ${request.effectType}")
        }

        val stripEntityOptional = stripRepository.findByUuid(request.stripUuid)
        return if (stripEntityOptional.isPresent) {
            val effectEntity = LightEffectEntity(
                strip = stripEntityOptional.get(),
                uuid = UUID.randomUUID().toString(),
                name = request.name,
                status = LightEffectStatus.Created,
                settings = request.settings
            )
            effectRepository.save(effectEntity)
            effectEntity.uuid!!
        } else {
            throw ClientRequestException("No strip found with uuid ${request.stripUuid}!")
        }
    }

    fun getEffectsForStrip(stripUuid: String): GetEffectsResponse {
        val stripEntityOptional = stripRepository.findByUuid(stripUuid)
        return if (stripEntityOptional.isPresent) {
            val strip = stripEntityOptional.get()
            val effectList = strip.effects.map {
                GetEffectResponse(
                    name = it.name!!,
                    uuid = it.uuid!!,
                    stripUuid = strip.uuid!!,
                    settings = it.settings!!,
                    status = it.status!!,
                )
            }
            GetEffectsResponse(effectList)
        } else {
            throw ClientRequestException("Could not get effects. Strip with uuid $stripUuid does not exist!")
        }
    }

    fun getAllEffects(): GetEffectsResponse {
        // TODO strip vs strip group differentiation, strip group support
        val effectEntities = effectRepository.queryAll()
        val effectList = effectEntities.map {
            GetEffectResponse(
                name = it.name!!,
                uuid = it.uuid!!,
                stripUuid = it.strip!!.uuid!!,
                settings = it.settings!!,
                status = it.status!!,
            )
        }

        return GetEffectsResponse(effectList)
    }
}