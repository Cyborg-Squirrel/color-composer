package io.cyborgsquirrel.lighting.job

import io.cyborgsquirrel.client_config.repository.H2GroupMemberLedStripRepository
import io.cyborgsquirrel.entity.LightEffectEntity
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
import io.cyborgsquirrel.lighting.effects.settings.ColorFillNightriderEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.SpectrumLightEffectSettings
import io.cyborgsquirrel.model.strip.LedStrip
import io.cyborgsquirrel.model.strip.LedStripGroupModel
import io.cyborgsquirrel.model.strip.LedStripModel
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
                        status = effectEntity.status!!,
                        strip = strip,
                        effect = effectInstance,
                        filters = listOf()
                    )

                    activeLightEffectRegistry.addOrUpdateEffect(activeEffect)

                    val trigger = getTrigger(effectEntity)
                    if (trigger != null) {
                        triggerManager.addTrigger(trigger)
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
            return LedStripModel(stripEntity.name!!, stripEntity.uuid!!, stripEntity.length!!, stripEntity.height)
        } else if (groupEntity != null) {
            // Query to do JOIN (effect entity JOIN doesn't capture led strips if it points to a group)
            val stripMemberEntities = groupMemberLedStripRepository.findByGroup(groupEntity)
            val stripEntities = stripMemberEntities.mapNotNull { it.strip }
            val stripModels = stripEntities.map {
                LedStripModel(it.name!!, it.uuid!!, it.length!!, it.height)
            }
            return LedStripGroupModel(groupEntity.name!!, groupEntity.uuid!!, stripModels)
        } else {
            throw Exception("Strip or group must be non-null!")
        }
    }

    private fun getEffect(effectEntity: LightEffectEntity, strip: LedStrip): LightEffect {
        return when (effectEntity.name!!) {
            LightEffectConstants.NIGHTRIDER_NAME -> NightriderLightEffect(
                numberOfLeds = strip.getLength(),
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(effectEntity.settings),
                    ColorFillNightriderEffectSettings::class.java
                )
            )

            LightEffectConstants.SPECTRUM_NAME -> SpectrumLightEffect(
                numberOfLeds = strip.getLength(),
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(effectEntity.settings),
                    SpectrumLightEffectSettings::class.java
                )
            )

            else -> throw IllegalArgumentException("Unknown LightEffect name: ${effectEntity.name}")
        }
    }

    // TODO: one effect multiple triggers support
    private fun getTrigger(effectEntity: LightEffectEntity): LightEffectTrigger? {
        return if (effectEntity.triggers.isEmpty()) {
            null
        } else {
            val trigger = effectEntity.triggers.first()
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

                else -> throw IllegalArgumentException("Unknown LightEffectTrigger name: ${trigger.name}")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LightEffectInitJob::class.java)
    }
}