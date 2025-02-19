package io.cyborgsquirrel.lighting.job

import io.cyborgsquirrel.client_config.repository.H2GroupMemberLedStripRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripGroupRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripRepository
import io.cyborgsquirrel.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.*
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.settings.NightriderLightEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.SpectrumLightEffectSettings
import io.cyborgsquirrel.model.strip.LedStrip
import io.cyborgsquirrel.model.strip.LedStripGroupModel
import io.cyborgsquirrel.model.strip.LedStripModel
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
    private val objectMapper: ObjectMapper,
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
                    val effectInstance = getEffectWithName(effectEntity, strip)
                    val activeEffect = ActiveLightEffect(
                        uuid = effectEntity.uuid!!,
                        priority = 0,
                        status = effectEntity.status!!,
                        strip = strip,
                        effect = effectInstance,
                        filters = listOf()
                    )

                    logger.info("New light effect ${effectInstance.getName()} on strip ${strip.getUuid()}")
                    activeLightEffectRegistry.addOrUpdateEffect(activeEffect)
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
            val stripEntities = stripMemberEntities.map { it.strip }.filterNotNull()
            val stripModels = stripEntities.map {
                LedStripModel(it.name!!, it.uuid!!, it.length!!, it.height)
            }
            return LedStripGroupModel(groupEntity.name!!, groupEntity.uuid!!, stripModels)
        } else {
            throw Exception("Strip or group must be non-null!")
        }
    }

    private fun getEffectWithName(effectEntity: LightEffectEntity, strip: LedStrip): LightEffect {
        return when (effectEntity.name!!) {
            LightEffectConstants.NIGHTRIDER_NAME -> NightriderLightEffect(
                numberOfLeds = strip.getLength(),
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(effectEntity.settings),
                    NightriderLightEffectSettings::class.java
                )
            )

            LightEffectConstants.ANIMATED_SPECTRUM_NAME -> AnimatedSpectrumLightEffect(
                numberOfLeds = strip.getLength(),
                settings = objectMapper.readValueFromTree(
                    JsonNode.from(effectEntity.settings),
                    SpectrumLightEffectSettings::class.java
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

    companion object {
        private val logger = LoggerFactory.getLogger(LightEffectInitJob::class.java)
    }
}