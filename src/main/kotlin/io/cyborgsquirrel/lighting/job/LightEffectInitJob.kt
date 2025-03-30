package io.cyborgsquirrel.lighting.job

import io.cyborgsquirrel.led_strips.repository.H2GroupMemberLedStripRepository
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effect_trigger.LightEffectTriggerConstants
import io.cyborgsquirrel.lighting.effect_trigger.TriggerManager
import io.cyborgsquirrel.lighting.effect_trigger.settings.EffectIterationTriggerSettings
import io.cyborgsquirrel.lighting.effect_trigger.settings.SunriseSunsetTriggerSettings
import io.cyborgsquirrel.lighting.effect_trigger.settings.TimeTriggerSettings
import io.cyborgsquirrel.lighting.effect_trigger.triggers.EffectIterationTrigger
import io.cyborgsquirrel.lighting.effect_trigger.triggers.LightEffectTrigger
import io.cyborgsquirrel.lighting.effect_trigger.triggers.SunriseSunsetTrigger
import io.cyborgsquirrel.lighting.effect_trigger.triggers.TimeTrigger
import io.cyborgsquirrel.lighting.effects.*
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.settings.NightriderColorFillEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.NightriderCometEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.SpectrumEffectSettings
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
import org.slf4j.LoggerFactory
import java.util.concurrent.Semaphore

@Singleton
class LightEffectInitJob(
    private val lightEffectRepository: H2LightEffectRepository,
    private val groupMemberLedStripRepository: H2GroupMemberLedStripRepository,
    private val activeLightEffectRegistry: ActiveLightEffectRegistry,
    private val sunriseSunsetTimeRepository: H2SunriseSunsetTimeRepository,
    private val locationConfigRepository: H2LocationConfigRepository,
    private val triggerManager: TriggerManager,
    private val objectMapper: ObjectMapper,
    private val timeHelper: TimeHelper,
) : Runnable {

    // Flag tracking if this init job has run.
    // Should only run once at startup.
    private var completed = false
    private val lock = Semaphore(1)

    override fun run() {
        try {
            lock.acquire()
            if (!completed) {
                val effectEntities = lightEffectRepository.queryAll()
                for (effectEntity in effectEntities) {
                    val strip = getLedStrip(effectEntity)
                    val effectInstance = getEffect(effectEntity, strip)
                    val activeEffect = ActiveLightEffect(
                        uuid = effectEntity.uuid!!,
                        // TODO add priority to persistence layer
                        priority = 0,
                        skipFramesIfBlank = true,
                        status = effectEntity.status!!,
                        strip = strip,
                        effect = effectInstance,
                        filters = listOf()
                    )

                    activeLightEffectRegistry.addOrUpdateEffect(activeEffect)

                    val triggers = getTriggers(effectEntity)
                    if (triggers.isNotEmpty()) {
                        triggers.forEach {
                            triggerManager.addTrigger(it)
                        }
                    }

                    val filters = getFilters(effectEntity)
                    if (filters.isNotEmpty()) {
                        activeLightEffectRegistry.addOrUpdateEffect(activeEffect.copy(filters = activeEffect.filters + filters))
                    }
                }

                completed = true
            }
        } catch (ex: java.lang.Exception) {
            logger.error("Error initializing light effects from database!", ex)
        } finally {
            lock.release()
        }
    }

    private fun getLedStrip(effectEntity: LightEffectEntity): LedStrip {
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

    private fun getEffect(effectEntity: LightEffectEntity, strip: LedStrip): LightEffect {
        return when (effectEntity.name!!) {
            LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME -> NightriderLightEffect(
                numberOfLeds = strip.getLength(),
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(effectEntity.settings),
                    NightriderColorFillEffectSettings::class.java
                )
            )

            LightEffectConstants.NIGHTRIDER_COMET_NAME -> NightriderLightEffect(
                numberOfLeds = strip.getLength(),
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(effectEntity.settings),
                    NightriderCometEffectSettings::class.java
                )
            )

            LightEffectConstants.SPECTRUM_NAME -> SpectrumLightEffect(
                numberOfLeds = strip.getLength(),
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(effectEntity.settings),
                    SpectrumEffectSettings::class.java
                )
            )

            else -> throw IllegalArgumentException("Unknown LightEffect name: ${effectEntity.name}")
        }
    }

    private fun getTriggers(effectEntity: LightEffectEntity): List<LightEffectTrigger> {
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

    private fun getFilters(effectEntity: LightEffectEntity): List<LightEffectFilter> {
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

    companion object {
        private val logger = LoggerFactory.getLogger(LightEffectInitJob::class.java)
    }
}