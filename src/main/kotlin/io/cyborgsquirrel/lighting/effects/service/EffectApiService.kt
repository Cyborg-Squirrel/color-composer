package io.cyborgsquirrel.lighting.effects.service

import io.cyborgsquirrel.event_source.model.LightEffectEvent
import io.cyborgsquirrel.event_source.service.SseEventEmitter
import io.cyborgsquirrel.led_strips.repository.LedStripPoolRepository
import io.cyborgsquirrel.led_strips.repository.LedStripRepository
import io.cyborgsquirrel.lighting.effect_palette.repository.LightEffectPaletteRepository
import io.cyborgsquirrel.lighting.effect_trigger.repository.LightEffectTriggerRepository
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.LightEffectType
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.repository.LightEffectRepository
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.LightEffectStatusCommand
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectStatusRequest
import io.cyborgsquirrel.lighting.effects.responses.GetEffectResponse
import io.cyborgsquirrel.lighting.effects.responses.GetEffectsResponse
import io.cyborgsquirrel.lighting.effects.responses.GetPoolEffectResponse
import io.cyborgsquirrel.lighting.effects.responses.GetStripEffectResponse
import io.cyborgsquirrel.lighting.effects.schemas.EffectSettingsSchema
import io.cyborgsquirrel.lighting.effects.schemas.EffectSettingsSchemaBuilder
import io.cyborgsquirrel.lighting.enums.EffectCategory
import io.cyborgsquirrel.lighting.enums.FadeCurve
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.filters.repository.LightEffectFilterRepository
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.cyborgsquirrel.util.exception.ResourceNotFoundException
import jakarta.inject.Singleton
import java.util.*

@Singleton
class EffectApiService(
    private val stripRepository: LedStripRepository,
    private val poolRepository: LedStripPoolRepository,
    private val effectRepository: LightEffectRepository,
    private val triggerRepository: LightEffectTriggerRepository,
    private val filterRepository: LightEffectFilterRepository,
    private val paletteRepository: LightEffectPaletteRepository,
    private val effectRegistry: LightEffectRegistry,
    private val createLightingService: CreateLightingService,
    private val sseEventEmitter: SseEventEmitter,
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

        val strip = createLightingService.ledStripFromEffectEntity(effectEntity)
        val palette = if (effectEntity.palette != null) createLightingService.createPalette(
            effectEntity.palette!!.settings!!,
            effectEntity.palette!!.type!!,
            effectEntity.palette!!.uuid!!,
            strip.length()
        ) else null
        val lightEffect = createLightingService.createEffect(
            effectEntity.settings, effectEntity.type, palette, strip.length()
        )

        val activeEffect = ActiveLightEffect(
            effectUuid = effectEntity.uuid,
            // TODO add priority to persistence layer
            priority = 0,
            skipFramesIfBlank = true,
            status = effectEntity.status!!,
            strip = strip,
            effect = lightEffect,
            filters = listOf()
        )

        effectRegistry.addOrUpdateEffect(activeEffect)
        sseEventEmitter.emit(LightEffectEvent.LightEffectCreated(effectEntity.uuid))
        return effectEntity.uuid
    }

    fun getEffectsForStrip(stripUuid: String): GetEffectsResponse {
        val stripEntityOptional = stripRepository.findByUuid(stripUuid)
        return if (stripEntityOptional.isPresent) {
            val stripEntity = stripEntityOptional.get()
            val effectEntities = effectRepository.findByStrip(stripEntity)
            val effectList = effectEntities.map {
                GetStripEffectResponse(
                    name = it.name,
                    uuid = it.uuid,
                    stripUuid = stripEntity.uuid!!,
                    paletteUuid = it.palette?.uuid,
                    settings = it.settings,
                    status = it.status!!,
                    type = it.type,
                    category = EffectCategory.forEffect(it.type),
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
                    name = it.name,
                    uuid = it.uuid,
                    poolUuid = poolEntity.uuid!!,
                    paletteUuid = it.palette?.uuid,
                    settings = it.settings,
                    status = it.status!!,
                    type = it.type,
                    category = EffectCategory.forEffect(it.type),
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
            val response = getEffectResponseForEffect(effectEntity)
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

            val activeEffect = effectRegistry.getEffectWithUuid(effectUuid)
            if (activeEffect != null) {
                effectRegistry.removeEffect(activeEffect)
            }
            sseEventEmitter.emit(LightEffectEvent.LightEffectDeleted(effectUuid))
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

            val stripUuid = updateEffectRequest.stripUuid
            val poolUuid = updateEffectRequest.poolUuid

            if (stripUuid != null && poolUuid != null) {
                throw ClientRequestException("Cannot assign effect to both a strip and a strip pool. Specify either 'stripUuid' or 'poolUuid'.")
            }

            if (stripUuid != null && updateEffectRequest.stripUuid != effectEntity.strip?.uuid) {
                val stripEntityOptional = stripRepository.findByUuid(updateEffectRequest.stripUuid)
                if (stripEntityOptional.isPresent) {
                    effectEntity = effectEntity.copy(
                        strip = stripEntityOptional.get()
                    )
                } else {
                    throw ClientRequestException("No LED strip with uuid ${updateEffectRequest.stripUuid}")
                }
            } else if (poolUuid != null && updateEffectRequest.poolUuid != effectEntity.pool?.uuid) {
                val poolEntityOptional = poolRepository.findByUuid(updateEffectRequest.poolUuid)
                if (poolEntityOptional.isPresent) {
                    effectEntity = effectEntity.copy(
                        pool = poolEntityOptional.get()
                    )
                } else {
                    throw ClientRequestException("No strip pool found with uuid ${updateEffectRequest.poolUuid}")
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

            var effectModel = effectRegistry.getEffectWithUuid(uuid)
            if (effectModel != null) {
                val strip = createLightingService.ledStripFromEffectEntity(effectEntity)
                val palette = if (effectEntity.palette != null) createLightingService.createPalette(
                    effectEntity.palette!!.settings!!,
                    effectEntity.palette!!.type!!,
                    effectEntity.palette!!.uuid!!,
                    strip.length()
                ) else null
                val lightEffect = createLightingService.createEffect(
                    effectEntity.settings, effectEntity.type, palette, strip.length()
                )

                val onlyPaletteChange =
                    updateEffectRequest.stripUuid == null && updateEffectRequest.paletteUuid != null && updateEffectRequest.name == null && updateEffectRequest.settings == null
                if (onlyPaletteChange && palette != null) {
                    // If the palette is the only update, don't create a new ActiveEffect with copy()
                    // This retains the state of the effect and just swaps the palette.
                    effectModel.effect.updatePalette(palette)
                } else {
                    effectModel = effectModel.copy(
                        effectUuid = effectEntity.uuid,
                        // TODO add priority to persistence layer
                        priority = 0,
                        skipFramesIfBlank = true,
                        status = effectEntity.status!!,
                        strip = strip,
                        effect = lightEffect,
                    )

                    effectRegistry.addOrUpdateEffect(effectModel)
                }
            }
            sseEventEmitter.emit(LightEffectEvent.LightEffectUpdated(uuid))
        } else {
            throw ResourceNotFoundException("Effect with uuid $uuid doesn't exist!")
        }
    }

    fun updateEffectStatus(request: UpdateEffectStatusRequest) {
        val effectEntities = mutableListOf<LightEffectEntity>()
        val activeEffects = mutableListOf<ActiveLightEffect>()
        request.uuids.forEach { uuid ->
            val effectEntityOptional = effectRepository.findByUuid(uuid)
            val activeEffect = effectRegistry.getEffectWithUuid(uuid)
            if (effectEntityOptional.isEmpty || activeEffect == null) {
                throw ClientRequestException("Effect with uuid $uuid doesn't exist!")
            } else {
                effectEntities.add(effectEntityOptional.get())
                activeEffects.add(activeEffect)
            }
        }

        for (entity in effectEntities) {
            val activeEffect = activeEffects.first { it.effectUuid == entity.uuid }
            val newStatus = effectStatusFromCommand(request.command)
            effectRegistry.addOrUpdateEffect(activeEffect.copy(status = newStatus))
            effectRepository.update(entity.copy(status = newStatus))
            sseEventEmitter.emit(LightEffectEvent.LightEffectUpdated(entity.uuid))
        }
    }

    private fun effectStatusFromCommand(command: LightEffectStatusCommand): LightEffectStatus {
        return when (command) {
            LightEffectStatusCommand.Play -> LightEffectStatus.Playing
            LightEffectStatusCommand.Pause -> LightEffectStatus.Paused
            LightEffectStatusCommand.Stop -> LightEffectStatus.Stopped
        }
    }

    fun getEffectResponseForEffect(lightEffectEntity: LightEffectEntity): GetEffectResponse? {
        return if (lightEffectEntity.strip != null) {
            GetStripEffectResponse(
                name = lightEffectEntity.name,
                uuid = lightEffectEntity.uuid,
                stripUuid = lightEffectEntity.strip!!.uuid!!,
                paletteUuid = lightEffectEntity.palette?.uuid,
                settings = lightEffectEntity.settings,
                status = lightEffectEntity.status!!,
                type = lightEffectEntity.type,
                category = EffectCategory.forEffect(lightEffectEntity.type),
            )
        } else if (lightEffectEntity.pool != null) {
            GetPoolEffectResponse(
                name = lightEffectEntity.name,
                uuid = lightEffectEntity.uuid,
                poolUuid = lightEffectEntity.pool!!.uuid!!,
                paletteUuid = lightEffectEntity.palette?.uuid,
                settings = lightEffectEntity.settings,
                status = lightEffectEntity.status!!,
                type = lightEffectEntity.type,
                category = EffectCategory.forEffect(lightEffectEntity.type),
            )
        } else {
            // TODO throw Exception? For now let the user get all effects even if there is one with an invalid config.
            null
        }
    }

    fun getAllSchemas(): List<EffectSettingsSchema> = LightEffectType.entries.map { effectType ->
        when (effectType) {
            LightEffectType.SPECTRUM ->
                EffectSettingsSchemaBuilder(effectType.displayName)
                    .integer("colorPixelWidth", "Number of pixels per color band") { min(1.0) }
                    .boolean("animated", "Whether the spectrum cycles through colors over time")
                    .integer("updatesPerSecond", "Number of animation steps per second") { min(1.0) }
                    .build()

            LightEffectType.NIGHTRIDER_COLOR_FILL ->
                EffectSettingsSchemaBuilder(effectType.displayName)
                    .boolean("wrap", "Whether the fill wraps around the strip ends")
                    .integer("updatesPerSecond", "Number of position updates per second") { min(1.0) }
                    .number("brightnessScaling", "Brightness multiplier applied to the effect") { min(0.0); max(1.0) }
                    .build()

            LightEffectType.NIGHTRIDER_COMET ->
                EffectSettingsSchemaBuilder(effectType.displayName)
                    .integer("trailLength", "Number of pixels in the comet's trailing tail") { min(1.0) }
                    .string("trailFadeCurve", "Brightness falloff curve along the trail") {
                        options(FadeCurve.entries.map { it.name })
                    }
                    .boolean("wrap", "Whether the comet wraps around the strip ends")
                    .integer("updatesPerSecond", "Number of position updates per second") { min(1.0) }
                    .build()

            LightEffectType.FLAME ->
                EffectSettingsSchemaBuilder(effectType.displayName)
                    .integer("cooling", "Rate at which heat dissipates up the strip") { min(1.0) }
                    .integer("sparking", "Probability of new sparks igniting at the base (0–255)") { min(0.0); max(255.0) }
                    .integer("sparks", "Number of sparks generated per update") { min(1.0) }
                    .integer("sparkHeight", "Maximum height sparks can reach from the base") { min(1.0) }
                    .integer("updatesPerSecond", "Number of fire simulation steps per second") { min(1.0) }
                    .build()

            LightEffectType.BOUNCING_BALL ->
                EffectSettingsSchemaBuilder(effectType.displayName)
                    .integer("startingHeightPercent", "Initial drop height as a percentage of strip length") { min(0.0); max(100.0) }
                    .integer("maxHeightPercent", "Maximum bounce height in pixels") { min(1.0) }
                    .number("speed", "Initial speed of the ball") { min(0.0) }
                    .number("gravity", "Gravitational acceleration applied to the ball")
                    .number("minimumSpeed", "Speed below which the ball stops bouncing") { min(0.0) }
                    .build()

            LightEffectType.WAVE ->
                EffectSettingsSchemaBuilder(effectType.displayName)
                    .integer("startPoint", "Starting pixel position of the wave") { min(0.0) }
                    .integer("waveLength", "Length of one full wave cycle in pixels") { min(1.0) }
                    .boolean("repeat", "Whether the wave repeats continuously")
                    .integer("updatesPerSecond", "Number of wave position steps per second") { min(1.0) }
                    .build()

            LightEffectType.MARQUEE ->
                EffectSettingsSchemaBuilder(effectType.displayName)
                    .integer("dotLength", "Length of each dot in pixels") { min(1.0) }
                    .integer("spaceBetweenDots", "Gap between dots in pixels") { min(0.0) }
                    .integer("updatesPerSecond", "Number of pixels the dots scroll per second") { min(1.0) }
                    .build()

            LightEffectType.SPARKLE ->
                EffectSettingsSchemaBuilder(effectType.displayName)
                    .integer("numDots", "Maximum number of simultaneous sparkle dots") { min(1.0) }
                    .integer("fadeInMillisMax", "Maximum fade-in duration in milliseconds") { min(1.0) }
                    .integer("fadeInMillisMin", "Minimum fade-in duration in milliseconds") { min(1.0) }
                    .integer("fadeOutMillisMax", "Maximum fade-out duration in milliseconds") { min(1.0) }
                    .integer("fadeOutMillisMin", "Minimum fade-out duration in milliseconds") { min(1.0) }
                    .integer("updatesPerSecond", "Number of sparkle state updates per second") { min(1.0) }
                    .build()
        }
    }
}
