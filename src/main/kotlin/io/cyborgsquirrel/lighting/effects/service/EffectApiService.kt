package io.cyborgsquirrel.lighting.effects.service

import io.cyborgsquirrel.led_strips.repository.H2LedStripPoolRepository
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.lighting.effect_palette.repository.H2LightEffectPaletteRepository
import io.cyborgsquirrel.lighting.effect_trigger.repository.H2LightEffectTriggerRepository
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.LightEffectStatusCommand
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectStatusRequest
import io.cyborgsquirrel.lighting.effects.responses.GetEffectResponse
import io.cyborgsquirrel.lighting.effects.responses.GetEffectsResponse
import io.cyborgsquirrel.lighting.effects.responses.GetPoolEffectResponse
import io.cyborgsquirrel.lighting.effects.responses.GetStripEffectResponse
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.filters.repository.H2LightEffectFilterRepository
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.cyborgsquirrel.util.exception.ResourceNotFoundException
import jakarta.inject.Singleton
import java.util.*

@Singleton
class EffectApiService(
    private val stripRepository: H2LedStripRepository,
    private val poolRepository: H2LedStripPoolRepository,
    private val effectRepository: H2LightEffectRepository,
    private val triggerRepository: H2LightEffectTriggerRepository,
    private val filterRepository: H2LightEffectFilterRepository,
    private val paletteRepository: H2LightEffectPaletteRepository,
    private val effectRegistry: ActiveLightEffectRegistry,
    private val createLightingService: CreateLightingService,
) {

    fun createEffect(request: CreateEffectRequest): String {
        val stripUuid = request.stripUuid
        val poolUuid = request.poolUuid

        if (stripUuid != null && poolUuid != null) {
            throw ClientRequestException("Cannot assign effect to both a strip and a strip pool. Please specify only either 'stripUuid' or 'poolUuid'.")
        }

        val paletteEntity = if (request.paletteUuid != null) {
            val paletteOptional = paletteRepository.findByUuid(request.paletteUuid)
            if (paletteOptional.isEmpty) {
                throw ClientRequestException("Palette with uuid ${request.paletteUuid} doesn't exist")
            }
            paletteOptional.get()
        } else {
            null
        }

        val effectEntity = when {
            stripUuid != null -> {
                val stripEntityOptional = stripRepository.findByUuid(stripUuid)
                if (stripEntityOptional.isPresent) {
                    effectRepository.save(
                        LightEffectEntity(
                            strip = stripEntityOptional.get(),
                            uuid = UUID.randomUUID().toString(),
                            name = request.name,
                            type = request.effectType,
                            status = LightEffectStatus.Idle,
                            settings = request.settings,
                            palette = paletteEntity
                        )
                    )
                } else {
                    throw ClientRequestException("No strip found with uuid ${request.stripUuid}!")
                }
            }

            poolUuid != null -> {
                val poolEntityOptional = poolRepository.findByUuid(poolUuid)
                if (poolEntityOptional.isPresent) {
                    effectRepository.save(
                        LightEffectEntity(
                            pool = poolEntityOptional.get(),
                            uuid = UUID.randomUUID().toString(),
                            name = request.name,
                            type = request.effectType,
                            status = LightEffectStatus.Idle,
                            settings = request.settings,
                            palette = paletteEntity
                        )
                    )
                } else {
                    throw ClientRequestException("No pool found with uuid ${request.stripUuid}!")
                }
            }

            else -> {
                throw ClientRequestException("'stripUuid' or 'poolUuid' must be specified!")
            }
        }

        // Common post‑creation logic
        val strip = createLightingService.ledStripFromEffectEntity(effectEntity)
        val palette = if (effectEntity.palette != null) createLightingService.createPalette(
            effectEntity.palette!!.settings!!,
            effectEntity.palette!!.type!!,
            effectEntity.palette!!.uuid!!,
            strip.length()
        ) else null
        val lightEffect = createLightingService.createEffect(
            effectEntity.settings!!, effectEntity.type!!, palette, strip.length()
        )

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
        return effectEntity.uuid!!
    }

    fun getEffectsForStrip(stripUuid: String): GetEffectsResponse {
        val stripEntityOptional = stripRepository.findByUuid(stripUuid)
        return if (stripEntityOptional.isPresent) {
            val stripEntity = stripEntityOptional.get()
            val effectEntities = effectRepository.findByStrip(stripEntity)
            val effectList = effectEntities.map {
                GetStripEffectResponse(
                    name = it.name!!,
                    uuid = it.uuid!!,
                    stripUuid = stripEntity.uuid!!,
                    paletteUuid = it.palette?.uuid,
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

    fun getEffectsForPool(poolUuid: String): GetEffectsResponse {
        val poolEntityOptional = poolRepository.findByUuid(poolUuid)
        return if (poolEntityOptional.isPresent) {
            val poolEntity = poolEntityOptional.get()
            val effectEntities = effectRepository.findByPool(poolEntity)
            val effectList = effectEntities.map {
                GetPoolEffectResponse(
                    name = it.name!!,
                    uuid = it.uuid!!,
                    poolUuid = poolEntity.uuid!!,
                    paletteUuid = it.palette?.uuid,
                    settings = it.settings!!,
                    status = it.status!!,
                    type = it.type!!,
                )
            }

            GetEffectsResponse(effectList)
        } else {
            throw ClientRequestException("Could not get effects. Pool with uuid $poolUuid does not exist!")
        }
    }

    fun getAllEffects(): GetEffectsResponse {
        // TODO strip vs strip pool differentiation, strip pool support
        val effectEntities = effectRepository.queryAll()
        val effectList: List<GetEffectResponse?> = effectEntities.map {
            getEffectResponseForEffect(it)
        }

        return GetEffectsResponse(effectList.filterNotNull())
    }

    fun getEffectWithUuid(uuid: String): GetEffectResponse {
        val effectEntityOptional = effectRepository.findByUuid(uuid)
        if (effectEntityOptional.isPresent) {
            val effectEntity = effectEntityOptional.get()
            val response =  getEffectResponseForEffect(effectEntity)
            if (response != null) {
                return response
            }

            throw ResourceNotFoundException("Error fetching effect with uuid $uuid!")
        } else {
            throw ResourceNotFoundException("Effect with uuid $uuid does not exist!")
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
            throw ResourceNotFoundException("Effect with uuid $effectUuid doesn't exist!")
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

            if (updateEffectRequest.paletteUuid != null && updateEffectRequest.paletteUuid != effectEntity.palette?.uuid) {
                val paletteEntityOptional = paletteRepository.findByUuid(updateEffectRequest.paletteUuid)
                if (paletteEntityOptional.isPresent) {
                    effectEntity = effectEntity.copy(
                        palette = paletteEntityOptional.get()
                    )
                } else {
                    throw ClientRequestException("No palette with uuid ${updateEffectRequest.paletteUuid}")
                }
            }

            effectEntity = effectRepository.update(effectEntity)

            val activeEffectOptional = effectRegistry.getEffectWithUuid(uuid)
            if (activeEffectOptional.isPresent) {
                val strip = createLightingService.ledStripFromEffectEntity(effectEntity)
                val palette = if (effectEntity.palette != null) createLightingService.createPalette(
                    effectEntity.palette!!.settings!!,
                    effectEntity.palette!!.type!!,
                    effectEntity.palette!!.uuid!!,
                    strip.length()
                ) else null
                val lightEffect = createLightingService.createEffect(
                    effectEntity.settings!!, effectEntity.type!!, palette, strip.length()
                )

                var activeEffect = activeEffectOptional.get()
                val onlyPaletteChange =
                    updateEffectRequest.stripUuid == null && updateEffectRequest.paletteUuid != null && updateEffectRequest.name == null && updateEffectRequest.settings == null
                if (onlyPaletteChange && palette != null) {
                    // If the palette is the only update, don't create a new ActiveEffect with copy()
                    // This retains the state of the effect and just swaps the palette.
                    activeEffect.effect.updatePalette(palette)
                } else {
                    activeEffect = activeEffect.copy(
                        effectUuid = effectEntity.uuid!!,
                        // TODO add priority to persistence layer
                        priority = 0,
                        skipFramesIfBlank = true,
                        status = effectEntity.status!!,
                        strip = strip,
                        effect = lightEffect,
                    )

                    effectRegistry.addOrUpdateEffect(activeEffect)
                }
            }
        } else {
            throw ResourceNotFoundException("Effect with uuid $uuid doesn't exist!")
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
            val newStatus = effectStatusFromCommand(request.command)
            effectRegistry.addOrUpdateEffect(activeEffect.copy(status = newStatus))
            effectRepository.update(entity.copy(status = newStatus))
        }
    }

    private fun effectStatusFromCommand(command: LightEffectStatusCommand): LightEffectStatus {
        return when (command) {
            LightEffectStatusCommand.Play -> LightEffectStatus.Playing
            LightEffectStatusCommand.Pause -> LightEffectStatus.Paused
            LightEffectStatusCommand.Stop -> LightEffectStatus.Stopped
        }
    }

    private fun getEffectResponseForEffect(lightEffectEntity: LightEffectEntity): GetEffectResponse? {
        return if (lightEffectEntity.strip != null) {
            GetStripEffectResponse(
                name = lightEffectEntity.name!!,
                uuid = lightEffectEntity.uuid!!,
                stripUuid = lightEffectEntity.strip!!.uuid!!,
                paletteUuid = lightEffectEntity.palette?.uuid,
                settings = lightEffectEntity.settings!!,
                status = lightEffectEntity.status!!,
                type = lightEffectEntity.type!!,
            )
        } else if (lightEffectEntity.pool != null) {
            GetPoolEffectResponse(
                name = lightEffectEntity.name!!,
                uuid = lightEffectEntity.uuid!!,
                poolUuid = lightEffectEntity.pool!!.uuid!!,
                paletteUuid = lightEffectEntity.palette?.uuid,
                settings = lightEffectEntity.settings!!,
                status = lightEffectEntity.status!!,
                type = lightEffectEntity.type!!,
            )
        } else {
            // TODO throw Exception? For now let the user get all effects even if there is one with an invalid config.
            null
        }
    }
}