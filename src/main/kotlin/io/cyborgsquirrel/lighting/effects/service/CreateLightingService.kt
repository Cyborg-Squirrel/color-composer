package io.cyborgsquirrel.lighting.effects.service

import io.cyborgsquirrel.led_strips.repository.H2GroupMemberLedStripRepository
import io.cyborgsquirrel.lighting.effect_palette.EffectPaletteConstants
import io.cyborgsquirrel.lighting.effect_palette.palette.*
import io.cyborgsquirrel.lighting.effect_palette.settings.*
import io.cyborgsquirrel.lighting.effect_trigger.LightEffectTriggerConstants
import io.cyborgsquirrel.lighting.effect_trigger.settings.EffectIterationTriggerSettings
import io.cyborgsquirrel.lighting.effect_trigger.settings.TimeOfDayTriggerSettings
import io.cyborgsquirrel.lighting.effect_trigger.settings.TimeTriggerSettings
import io.cyborgsquirrel.lighting.effect_trigger.triggers.EffectIterationTrigger
import io.cyborgsquirrel.lighting.effect_trigger.triggers.LightEffectTrigger
import io.cyborgsquirrel.lighting.effect_trigger.triggers.TimeOfDayTrigger
import io.cyborgsquirrel.lighting.effect_trigger.triggers.TimeTrigger
import io.cyborgsquirrel.lighting.effects.*
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.effects.settings.*
import io.cyborgsquirrel.lighting.filters.*
import io.cyborgsquirrel.lighting.filters.repository.H2LightEffectFilterJunctionRepository
import io.cyborgsquirrel.lighting.filters.settings.BrightnessFadeFilterSettings
import io.cyborgsquirrel.lighting.filters.settings.BrightnessFilterSettings
import io.cyborgsquirrel.lighting.filters.settings.ReflectionFilterSettings
import io.cyborgsquirrel.lighting.model.LedStrip
import io.cyborgsquirrel.lighting.model.LedStripGroupModel
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.sunrise_sunset.repository.H2LocationConfigRepository
import io.cyborgsquirrel.sunrise_sunset.repository.H2SunriseSunsetTimeRepository
import io.cyborgsquirrel.util.time.TimeHelper
import io.cyborgsquirrel.util.time.TimeOfDayService
import io.micronaut.json.tree.JsonNode
import io.micronaut.serde.ObjectMapper
import jakarta.inject.Singleton

@Singleton
class CreateLightingService(
    private val groupMemberLedStripRepository: H2GroupMemberLedStripRepository,
    private val sunriseSunsetTimeRepository: H2SunriseSunsetTimeRepository,
    private val locationConfigRepository: H2LocationConfigRepository,
    private val junctionRepository: H2LightEffectFilterJunctionRepository,
    private val activeLightEffectRegistry: ActiveLightEffectRegistry,
    private val timeHelper: TimeHelper,
    private val timeOfDayService: TimeOfDayService,
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

    fun createPalette(
        settings: Map<String, Any>,
        paletteType: String,
        uuid: String,
        strip: LedStrip
    ): ColorPalette {
        return when (paletteType) {
            EffectPaletteConstants.STATIC_COLOR_PALETTE -> {
                StaticColorPalette(
                    settings = objectMapper.readValueFromTree(
                        JsonNode.from(settings),
                        StaticPaletteSettings::class.java
                    ),
                    uuid = uuid,
                    strip = strip,
                )
            }

            EffectPaletteConstants.GRADIENT_COLOR_PALETTE_NAME -> {
                GradientColorPalette(
                    settings = objectMapper.readValueFromTree(
                        JsonNode.from(settings),
                        GradientPaletteSettings::class.java
                    ),
                    uuid = uuid,
                    strip = strip,
                )
            }

            EffectPaletteConstants.CHANGING_COLOR_GRADIENT_PALETTE_NAME -> {
                ChangingColorPalette(
                    settings = objectMapper.readValueFromTree(
                        JsonNode.from(settings),
                        ChangingGradientPaletteSettings::class.java
                    ),
                    timeHelper = timeHelper,
                    uuid = uuid,
                    strip = strip,
                )
            }

            EffectPaletteConstants.CHANGING_COLOR_STATIC_PALETTE_NAME -> {
                ChangingColorPalette(
                    settings = objectMapper.readValueFromTree(
                        JsonNode.from(settings),
                        ChangingStaticPaletteSettings::class.java
                    ),
                    timeHelper = timeHelper,
                    uuid = uuid,
                    strip = strip,
                )
            }

            EffectPaletteConstants.TIME_OF_DAY_COLOR_PALETTE -> {
                TimeOfDayColorPalette(
                    settings = objectMapper.readValueFromTree(
                        JsonNode.from(settings),
                        TimeOfDayPaletteSettings::class.java
                    ),
                    timeHelper = timeHelper,
                    timeOfDayService = timeOfDayService,
                    locationConfigRepository = locationConfigRepository,
                    sunriseSunsetTimeRepository = sunriseSunsetTimeRepository,
                    objectMapper = objectMapper,
                    uuid = uuid,
                    strip = strip,
                )
            }

            EffectPaletteConstants.LOCAL_TIME_COLOR_PALETTE -> {
                LocalTimeColorPalette(
                    settings = objectMapper.readValueFromTree(
                        JsonNode.from(settings),
                        LocalTimePaletteSettings::class.java
                    ),
                    timeHelper = timeHelper,
                    uuid = uuid,
                    strip = strip,
                )
            }

            else -> throw IllegalArgumentException("Unknown LightEffect name: $paletteType")
        }
    }

    fun createEffect(
        settings: Map<String, Any>,
        effectType: String,
        palette: ColorPalette?,
        strip: LedStrip
    ): LightEffect {
        return when (effectType) {
            LightEffectConstants.BOUNCING_BALL_NAME -> BouncingBallLightEffect(
                numberOfLeds = strip.getLength(),
                timeHelper = timeHelper,
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    BouncingBallEffectSettings::class.java
                ),
                palette
            )

            LightEffectConstants.FLAME_EFFECT_NAME -> FlameLightEffect(
                numberOfLeds = strip.getLength(),
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    FlameEffectSettings::class.java
                ),
                palette
            )

            LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME -> NightriderLightEffect(
                numberOfLeds = strip.getLength(),
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    NightriderColorFillEffectSettings::class.java
                ),
                palette
            )

            LightEffectConstants.NIGHTRIDER_COMET_NAME -> NightriderLightEffect(
                numberOfLeds = strip.getLength(),
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    NightriderCometEffectSettings::class.java
                ),
                palette
            )

            LightEffectConstants.SPECTRUM_NAME -> SpectrumLightEffect(
                numberOfLeds = strip.getLength(),
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    SpectrumEffectSettings::class.java
                ),
                palette
            )

            LightEffectConstants.WAVE_EFFECT_NAME -> WaveLightEffect(
                numberOfLeds = strip.getLength(),
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    WaveEffectSettings::class.java
                ),
                palette
            )

            LightEffectConstants.MARQUEE_EFFECT_NAME -> MarqueeEffect(
                numberOfLeds = strip.getLength(),
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(settings),
                    MarqueeEffectSettings::class.java
                ),
                palette,
                timeHelper
            )

            else -> throw IllegalArgumentException("Unknown LightEffect name: $effectType")
        }
    }

    fun effectTriggerFromEntity(effectEntity: LightEffectEntity): List<LightEffectTrigger> {
        return if (effectEntity.triggers.isEmpty()) {
            listOf()
        } else {
            effectEntity.triggers.map { trigger ->
                when (trigger.type) {
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

                    LightEffectTriggerConstants.TIME_OF_DAY_TRIGGER_NAME -> {
                        TimeOfDayTrigger(
                            sunriseSunsetTimeRepository = sunriseSunsetTimeRepository,
                            locationConfigRepository = locationConfigRepository,
                            objectMapper = objectMapper,
                            timeHelper = timeHelper,
                            timeOfDayService = timeOfDayService,
                            settings = objectMapper.readValueFromTree(
                                JsonNode.from(trigger.settings),
                                TimeOfDayTriggerSettings::class.java
                            ),
                            uuid = trigger.uuid!!,
                            effectUuid = effectEntity.uuid!!
                        )
                    }

                    // TODO throwing an Exception is not ideal. Logger.error instead?
                    else -> throw IllegalArgumentException("Unknown LightEffectTrigger name: ${trigger.name}")
                }
            }
        }
    }

    fun createEffectFilterFromEntity(effectEntity: LightEffectEntity): List<LightEffectFilter> {
        return if (effectEntity.filterJunctions.isEmpty()) {
            listOf()
        } else {
            val junctionEntities = junctionRepository.findByEffect(effectEntity)
            val filterEntities = junctionEntities.map { it.filter!! }
            filterEntities.map { filter ->
                createEffectFilter(filter.type!!, filter.uuid!!, filter.settings!!)
            }
        }
    }

    fun createEffectFilter(effectType: String, uuid: String, settings: Map<String, Any>): LightEffectFilter {
        return when (effectType) {
            LightEffectFilterConstants.BRIGHTNESS_FADE_FILTER_NAME -> {
                BrightnessFadeFilter(
                    timeHelper = timeHelper,
                    settings = objectMapper.readValueFromTree(
                        JsonNode.from(settings),
                        BrightnessFadeFilterSettings::class.java
                    ),
                    uuid = uuid
                )
            }

            LightEffectFilterConstants.BRIGHTNESS_FILTER_NAME -> {
                BrightnessFilter(
                    settings = objectMapper.readValueFromTree(
                        JsonNode.from(settings),
                        BrightnessFilterSettings::class.java
                    ),
                    uuid = uuid
                )
            }

            LightEffectFilterConstants.REFLECTION_FILTER_NAME -> {
                ReflectionFilter(
                    settings = objectMapper.readValueFromTree(
                        JsonNode.from(settings),
                        ReflectionFilterSettings::class.java
                    ),
                    uuid = uuid
                )
            }

            LightEffectFilterConstants.REVERSE_FILTER_NAME -> {
                // Reverse filter doesn't have settings
                ReverseFilter(uuid = uuid)
            }

            // TODO throwing an Exception is not ideal. Logger.error instead?
            else -> throw IllegalArgumentException("Unknown LightEffectFilter type: $effectType")
        }
    }
}