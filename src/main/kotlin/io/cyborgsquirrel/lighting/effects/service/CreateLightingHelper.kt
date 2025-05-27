package io.cyborgsquirrel.lighting.effects.service

import io.cyborgsquirrel.led_strips.repository.H2GroupMemberLedStripRepository
import io.cyborgsquirrel.lighting.effect_trigger.LightEffectTriggerConstants
import io.cyborgsquirrel.lighting.effect_trigger.settings.EffectIterationTriggerSettings
import io.cyborgsquirrel.lighting.effect_trigger.settings.SunriseSunsetTriggerSettings
import io.cyborgsquirrel.lighting.effect_trigger.settings.TimeTriggerSettings
import io.cyborgsquirrel.lighting.effect_trigger.triggers.EffectIterationTrigger
import io.cyborgsquirrel.lighting.effect_trigger.triggers.LightEffectTrigger
import io.cyborgsquirrel.lighting.effect_trigger.triggers.SunriseSunsetTrigger
import io.cyborgsquirrel.lighting.effect_trigger.triggers.TimeTrigger
import io.cyborgsquirrel.lighting.effects.*
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.effects.settings.*
import io.cyborgsquirrel.lighting.filters.*
import io.cyborgsquirrel.lighting.filters.settings.BrightnessFadeFilterSettings
import io.cyborgsquirrel.lighting.filters.settings.BrightnessFilterSettings
import io.cyborgsquirrel.lighting.filters.settings.ReflectionFilterSettings
import io.cyborgsquirrel.lighting.model.LedStrip
import io.cyborgsquirrel.lighting.model.LedStripGroupModel
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.sunrise_sunset.repository.H2LocationConfigRepository
import io.cyborgsquirrel.sunrise_sunset.repository.H2SunriseSunsetTimeRepository
import io.cyborgsquirrel.util.time.TimeHelper
import io.micronaut.json.tree.JsonNode
import io.micronaut.serde.ObjectMapper
import jakarta.inject.Singleton

@Singleton
class CreateLightingHelper(
    private val groupMemberLedStripRepository: H2GroupMemberLedStripRepository,
    private val sunriseSunsetTimeRepository: H2SunriseSunsetTimeRepository,
    private val locationConfigRepository: H2LocationConfigRepository,
    private val activeLightEffectRegistry: ActiveLightEffectRegistry,
    private val timeHelper: TimeHelper,
    private val objectMapper: ObjectMapper
) {

    fun ledStripFromEffectEntity(effectEntity: LightEffectEntity): LedStrip {
        val stripEntity = effectEntity.strip
        val groupEntity = effectEntity.group

        if (stripEntity != null) {
            return LedStripModel(
                stripEntity.name!!,
                stripEntity.uuid!!,
                stripEntity.length!!,
                stripEntity.height,
                stripEntity.blendMode!!
            )
        } else if (groupEntity != null) {
            // Query to do JOIN (effect entity JOIN doesn't capture led strips if it points to a group)
            val stripMemberEntities = groupMemberLedStripRepository.findByGroup(groupEntity)
            val stripEntities = stripMemberEntities.mapNotNull { it.strip }
            val stripModels = stripEntities.map {
                LedStripModel(it.name!!, it.uuid!!, it.length!!, it.height, it.blendMode!!)
            }
            return LedStripGroupModel(
                groupEntity.name!!,
                groupEntity.uuid!!,
                stripModels,
                stripModels.first().getBlendMode()
            )
        } else {
            throw Exception("Strip or group must be non-null!")
        }
    }

    fun createEffect(settings: Map<String, Any>, effectName: String, stripLength: Int): LightEffect {
        return when (effectName) {
            LightEffectConstants.BOUNCING_BALL_NAME -> BouncingBallLightEffect(
                numberOfLeds = stripLength,
                timeHelper = timeHelper,
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    BouncingBallEffectSettings::class.java
                )
            )

            LightEffectConstants.FLAME_EFFECT_NAME -> FlameLightEffect(
                numberOfLeds = stripLength,
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    FlameEffectSettings::class.java
                )
            )

            LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME -> NightriderLightEffect(
                numberOfLeds = stripLength,
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    NightriderColorFillEffectSettings::class.java
                )
            )

            LightEffectConstants.NIGHTRIDER_COMET_NAME -> NightriderLightEffect(
                numberOfLeds = stripLength,
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    NightriderCometEffectSettings::class.java
                )
            )

            LightEffectConstants.SPECTRUM_NAME -> SpectrumLightEffect(
                numberOfLeds = stripLength,
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    SpectrumEffectSettings::class.java
                )
            )

            // TODO WaveLightEffect settings
            LightEffectConstants.WAVE_EFFECT_NAME -> WaveLightEffect(
                numberOfLeds = stripLength,
                startPoint = 0,
                waveLength = 1,
            )

            else -> throw IllegalArgumentException("Unknown LightEffect name: $effectName")
        }
    }

    fun effectTriggerFromEntity(effectEntity: LightEffectEntity): List<LightEffectTrigger> {
        return if (effectEntity.triggers.isEmpty()) {
            listOf()
        } else {
            effectEntity.triggers.map { trigger ->
                when (trigger.name) {
                    LightEffectTriggerConstants.ITERATION_TRIGGER_NAME -> {
                        EffectIterationTrigger(
                            timeHelper = timeHelper,
                            effectRegistry = activeLightEffectRegistry,
                            settings = objectMapper.readValueFromTree(
                                JsonNode.from(trigger.settings),
                                EffectIterationTriggerSettings::class.java
                            ),
                            uuid = trigger.uuid!!,
                            effectUuid = effectEntity.uuid!!,
                        )
                    }

                    LightEffectTriggerConstants.TIME_TRIGGER_NAME -> {
                        TimeTrigger(
                            timeHelper = timeHelper,
                            settings = objectMapper.readValueFromTree(
                                JsonNode.from(trigger.settings),
                                TimeTriggerSettings::class.java
                            ),
                            uuid = trigger.uuid!!,
                            effectUuid = effectEntity.uuid!!,
                        )
                    }

                    LightEffectTriggerConstants.SUNRISE_SUNSET_TRIGGER_NAME -> {
                        SunriseSunsetTrigger(
                            sunriseSunsetTimeRepository = sunriseSunsetTimeRepository,
                            locationConfigRepository = locationConfigRepository,
                            objectMapper = objectMapper,
                            timeHelper = timeHelper,
                            settings = objectMapper.readValueFromTree(
                                JsonNode.from(trigger.settings),
                                SunriseSunsetTriggerSettings::class.java
                            ),
                            uuid = trigger.uuid!!,
                            effectUuid = effectEntity.uuid!!,
                        )
                    }

                    // TODO throwing an Exception is not ideal. Logger.error instead?
                    else -> throw IllegalArgumentException("Unknown LightEffectTrigger name: ${trigger.name}")
                }
            }
        }
    }

    fun effectFilterFromEntity(effectEntity: LightEffectEntity): List<LightEffectFilter> {
        return if (effectEntity.filters.isEmpty()) {
            listOf()
        } else {
            effectEntity.filters.map { filter ->
                when (filter.name) {
                    LightEffectFilterConstants.BRIGHTNESS_FADE_FILTER_NAME -> {
                        BrightnessFadeFilter(
                            timeHelper = timeHelper,
                            settings = objectMapper.readValueFromTree(
                                JsonNode.from(filter.settings),
                                BrightnessFadeFilterSettings::class.java
                            ),
                            uuid = filter.uuid!!
                        )
                    }

                    LightEffectFilterConstants.BRIGHTNESS_FILTER_NAME -> {
                        BrightnessFilter(
                            settings = objectMapper.readValueFromTree(
                                JsonNode.from(filter.settings),
                                BrightnessFilterSettings::class.java
                            ),
                            uuid = filter.uuid!!
                        )
                    }

                    LightEffectFilterConstants.REFLECTION_FILTER_NAME -> {
                        ReflectionFilter(
                            settings = objectMapper.readValueFromTree(
                                JsonNode.from(filter.settings),
                                ReflectionFilterSettings::class.java
                            ),
                            uuid = filter.uuid!!
                        )
                    }

                    LightEffectFilterConstants.REVERSE_FILTER_NAME -> {
                        // Reverse filter doesn't have settings
                        ReverseFilter(uuid = filter.uuid!!)
                    }

                    // TODO throwing an Exception is not ideal. Logger.error instead?
                    else -> throw IllegalArgumentException("Unknown LightEffectFilter name: ${filter.name}")
                }
            }
        }
    }
}