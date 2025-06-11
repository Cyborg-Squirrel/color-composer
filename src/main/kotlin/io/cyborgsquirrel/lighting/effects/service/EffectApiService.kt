package io.cyborgsquirrel.lighting.effects.service

import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.lighting.effect_trigger.repository.H2LightEffectTriggerRepository
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectStatusRequest
import io.cyborgsquirrel.lighting.effects.responses.GetEffectResponse
import io.cyborgsquirrel.lighting.effects.responses.GetEffectsResponse
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.filters.repository.H2LightEffectFilterRepository
import io.cyborgsquirrel.util.exception.ClientRequestException
import jakarta.inject.Singleton
import java.util.*

@Singleton
class EffectApiService(
    private val stripRepository: H2LedStripRepository,
    private val effectRepository: H2LightEffectRepository,
    private val triggerRepository: H2LightEffectTriggerRepository,
    private val filterRepository: H2LightEffectFilterRepository,
    private val effectRegistry: ActiveLightEffectRegistry,
    private val createLightingHelper: CreateLightingHelper,
) {

    fun createEffect(request: CreateEffectRequest): String {
        val stripEntityOptional = stripRepository.findByUuid(request.stripUuid)
        return if (stripEntityOptional.isPresent) {
            val lightEffect =
                createLightingHelper.createEffect(
                    request.settings,
                    request.effectType,
                    stripEntityOptional.get().length!!
                )

            val effectEntity = effectRepository.save(
                LightEffectEntity(
                    strip = stripEntityOptional.get(),
                    uuid = UUID.randomUUID().toString(),
                    name = request.name,
                    type = request.effectType,
                    status = LightEffectStatus.Idle,
                    settings = request.settings
                )
            )

            val strip = createLightingHelper.ledStripFromEffectEntity(effectEntity)
            val activeEffect = ActiveLightEffect(
                effectUuid = effectEntity.uuid!!,
                // TODO add priority to persistence layer
                priority = 0,
                skipFramesIfBlank = true,
                status = effectEntity.status!!,
                strip = strip,
                effect = lightEffect,
                filters = listOf()
            )
            effectRegistry.addOrUpdateEffect(activeEffect)

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
                    type = it.type!!,
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
                type = it.type!!,
            )
        }

        return GetEffectsResponse(effectList)
    }

    fun getEffectWithUuid(uuid: String): GetEffectResponse {
        // TODO strip vs strip group differentiation, strip group support
        val effectEntityOptional = effectRepository.findByUuid(uuid)
        if (effectEntityOptional.isPresent) {
            val effectEntity = effectEntityOptional.get()
            return GetEffectResponse(
                name = effectEntity.name!!,
                uuid = effectEntity.uuid!!,
                stripUuid = effectEntity.strip!!.uuid!!,
                settings = effectEntity.settings!!,
                status = effectEntity.status!!,
                type = effectEntity.type!!,
            )
        } else {
            throw ClientRequestException("Effect with uuid $uuid does not exist!")
        }

    }

    fun deleteEffect(effectUuid: String) {
        val effectEntityOptional = effectRepository.findByUuid(effectUuid)
        if (effectEntityOptional.isPresent) {
            val effectEntity = effectEntityOptional.get()
            val filterIds = effectEntity.filterJunctions.map { it.filter!!.id }
            val filterEntities = filterRepository.findByIdIn(filterIds)
            val triggerEntities = triggerRepository.findByEffect(effectEntity)

            filterEntities.forEach { filterRepository.delete(it) }
            triggerEntities.forEach { triggerRepository.delete(it) }
            effectRepository.delete(effectEntity)

            val activeEffectOptional = effectRegistry.getEffectWithUuid(effectUuid)
            if (activeEffectOptional.isPresent) {
                effectRegistry.removeEffect(activeEffectOptional.get())
            }
        } else {
            throw ClientRequestException("Effect with uuid $effectUuid doesn't exist!")
        }
    }

    fun updateEffect(uuid: String, updateEffectRequest: UpdateEffectRequest) {
        val effectEntityOptional = effectRepository.findByUuid(uuid)
        if (effectEntityOptional.isPresent) {
            var effectEntity = effectEntityOptional.get()
            if (!updateEffectRequest.name.isNullOrBlank()) {
                effectEntity = effectEntity.copy(
                    name = updateEffectRequest.name
                )
            }

            if (updateEffectRequest.settings != null) {
                effectEntity = effectEntity.copy(
                    settings = updateEffectRequest.settings
                )
            }

            if (updateEffectRequest.status != null) {
                // Invalid statuses for an update request
                val invalidStatusList = listOf(LightEffectStatus.Idle)
                val validStatusList =
                    LightEffectStatus.entries.filter { !invalidStatusList.contains(it) }
                if (invalidStatusList.contains(updateEffectRequest.status)) {
                    throw ClientRequestException(
                        "Updating to status ${updateEffectRequest.status} is not allowed. Use $validStatusList."
                    )
                }

                effectEntity = effectEntity.copy(status = updateEffectRequest.status)
            }

            if (updateEffectRequest.stripUuid != null && updateEffectRequest.stripUuid != effectEntity.strip?.uuid) {
                val stripEntityOptional = stripRepository.findByUuid(updateEffectRequest.stripUuid)
                if (stripEntityOptional.isPresent) {
                    effectEntity = effectEntity.copy(
                        strip = stripEntityOptional.get()
                    )
                } else {
                    throw ClientRequestException("No LED strip with uuid ${updateEffectRequest.stripUuid}")
                }
            }

            val activeEffectOptional = effectRepository.findByUuid(uuid)
            if (activeEffectOptional.isPresent) {
                val lightEffect =
                    createLightingHelper.createEffect(
                        effectEntity.settings!!,
                        effectEntity.type!!,
                        effectEntity.strip!!.length!!
                    )
                val strip = createLightingHelper.ledStripFromEffectEntity(effectEntity)
                val activeEffect = ActiveLightEffect(
                    effectUuid = effectEntity.uuid!!,
                    // TODO add priority to persistence layer
                    priority = 0,
                    skipFramesIfBlank = true,
                    status = effectEntity.status!!,
                    strip = strip,
                    effect = lightEffect,
                    filters = listOf()
                )
                effectRegistry.addOrUpdateEffect(activeEffect)
            }

            effectRepository.update(effectEntity)
        } else {
            throw ClientRequestException("Effect with uuid $uuid doesn't exist!")
        }
    }

    fun updateEffectStatus(request: UpdateEffectStatusRequest) {
        val effectEntities = mutableListOf<LightEffectEntity>()
        val activeEffects = mutableListOf<ActiveLightEffect>()
        request.uuids.forEach { uuid ->
            val effectEntityOptional = effectRepository.findByUuid(uuid)
            val activeEffectOptional = effectRegistry.getEffectWithUuid(uuid)
            if (effectEntityOptional.isEmpty || activeEffectOptional.isEmpty) {
                throw ClientRequestException("Effect with uuid $uuid doesn't exist!")
            } else {
                effectEntities.add(effectEntityOptional.get())
                activeEffects.add(activeEffectOptional.get())
            }
        }

        for (entity in effectEntities) {
            val activeEffect = activeEffects.first { it.effectUuid == entity.uuid }
            effectRegistry.addOrUpdateEffect(activeEffect.copy(status = request.status))
            effectRepository.update(entity.copy(status = request.status))
        }
    }
}