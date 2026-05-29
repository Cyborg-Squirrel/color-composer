package io.cyborgsquirrel.lighting.effects.service

import io.cyborgsquirrel.event_source.model.EffectSettingsEvent
import io.cyborgsquirrel.event_source.model.LightEffectEvent
import io.cyborgsquirrel.event_source.service.SseEventEmitter
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.entity.LedStripPoolEntity
import io.cyborgsquirrel.led_strips.repository.LedStripPoolRepository
import io.cyborgsquirrel.led_strips.repository.LedStripRepository
import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effect_palette.repository.LightEffectPaletteRepository
import io.cyborgsquirrel.lighting.effect_settings.entity.LightEffectSettingsEntity
import io.cyborgsquirrel.lighting.effect_settings.repository.LightEffectSettingsRepository
import io.cyborgsquirrel.lighting.effect_trigger.repository.LightEffectTriggerRepository
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.LightEffect
import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.cyborgsquirrel.lighting.effects.LightEffectType
import io.cyborgsquirrel.lighting.effects.entity.LightEffectEntity
import io.cyborgsquirrel.lighting.effects.repository.LightEffectRepository
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectSettingsRequest
import io.cyborgsquirrel.lighting.effects.requests.LightEffectStatusCommand
import io.cyborgsquirrel.lighting.effects.requests.ReassignEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectSettingsRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectStatusRequest
import io.cyborgsquirrel.lighting.effects.responses.GetAllEffectSettingsResponse
import io.cyborgsquirrel.lighting.effects.responses.GetEffectResponse
import io.cyborgsquirrel.lighting.effects.responses.GetEffectSettingsResponse
import io.cyborgsquirrel.lighting.effects.responses.GetEffectsResponse
import io.cyborgsquirrel.lighting.effects.responses.GetPoolEffectResponse
import io.cyborgsquirrel.lighting.effects.responses.GetStripEffectResponse
import io.cyborgsquirrel.lighting.effects.responses.GetUnassignedEffectResponse
import io.cyborgsquirrel.lighting.effects.schemas.EffectSettingsSchema
import io.cyborgsquirrel.lighting.effects.schemas.EffectSettingsSchemaBuilder
import io.cyborgsquirrel.lighting.effects.settings.BouncingBallEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.FlameEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.MarqueeEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.NightriderColorFillEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.NightriderCometEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.SparkleEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.SpectrumEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.WaveEffectSettings
import io.cyborgsquirrel.lighting.enums.EffectCategory
import io.cyborgsquirrel.lighting.enums.FadeCurve
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.filters.repository.LightEffectFilterRepository
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.cyborgsquirrel.util.exception.ResourceNotFoundException
import io.micronaut.serde.ObjectMapper
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.util.*

@Singleton
open class EffectApiService(
    private val stripRepository: LedStripRepository,
    private val poolRepository: LedStripPoolRepository,
    private val effectRepository: LightEffectRepository,
    private val triggerRepository: LightEffectTriggerRepository,
    private val filterRepository: LightEffectFilterRepository,
    private val paletteRepository: LightEffectPaletteRepository,
    private val settingsRepository: LightEffectSettingsRepository,
    private val effectRegistry: LightEffectRegistry,
    private val createLightingService: CreateLightingService,
    private val sseEventEmitter: SseEventEmitter,
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    open fun createEffect(request: CreateEffectRequest): String {
        val stripUuid = request.stripUuid
        val poolUuid = request.poolUuid

        if (stripUuid != null && poolUuid != null) {
            throw ClientRequestException("Cannot assign effect to both a strip and a strip pool. Create request must only specify either 'stripUuid' or 'poolUuid'.")
        } else if (request.settingsUuid != null && request.settings != null) {
            throw ClientRequestException("A settings uuid and settings object were included in the create request. Create request must only specify either 'settingsUuid' or 'settings'.")
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

        val settingsEntity = resolveSettingsOnCreate(request.settingsUuid, request.effectType, request.settings)

        val effectEntity = when {
            stripUuid != null -> {
                val stripEntity = stripRepository.findByUuid(stripUuid)
                    .orElseThrow { ClientRequestException("No strip found with uuid $stripUuid!") }
                val count = effectRepository.findByStrip(stripEntity).size
                val targetLayer = request.layer ?: count
                validateLayerForInsert(targetLayer, count, "strip ${stripEntity.uuid}")
                if (targetLayer < count) {
                    shiftLayersUp(strip = stripEntity, pool = null, fromLayer = targetLayer)
                }
                val saved = effectRepository.save(
                    LightEffectEntity(
                        strip = stripEntity,
                        uuid = UUID.randomUUID().toString(),
                        name = request.name,
                        status = LightEffectStatus.Inactive,
                        effectSettings = settingsEntity,
                        palette = paletteEntity,
                        layer = targetLayer,
                    )
                )
                refreshRegistryLayers(strip = stripEntity, pool = null)
                saved
            }

            poolUuid != null -> {
                val poolEntity = poolRepository.findByUuid(poolUuid)
                    .orElseThrow { ClientRequestException("No pool found with uuid $poolUuid!") }
                val count = effectRepository.findByPool(poolEntity).size
                val targetLayer = request.layer ?: count
                validateLayerForInsert(targetLayer, count, "pool ${poolEntity.uuid}")
                if (targetLayer < count) {
                    shiftLayersUp(strip = null, pool = poolEntity, fromLayer = targetLayer)
                }
                val saved = effectRepository.save(
                    LightEffectEntity(
                        pool = poolEntity,
                        uuid = UUID.randomUUID().toString(),
                        name = request.name,
                        status = LightEffectStatus.Inactive,
                        palette = paletteEntity,
                        effectSettings = settingsEntity,
                        layer = targetLayer,
                    )
                )
                refreshRegistryLayers(strip = null, pool = poolEntity)
                saved
            }

            else -> {
                throw ClientRequestException("'stripUuid' or 'poolUuid' must be specified!")
            }
        }

        val strip = createLightingService.ledStripFromEffectEntity(effectEntity)
        val palette = if (effectEntity.palette != null) createLightingService.createPalette(
            effectEntity.palette!!.settings,
            effectEntity.palette!!.type,
            effectEntity.palette!!.uuid,
            strip.length()
        ) else null
        val lightEffect = createLightingService.createEffect(
            settingsEntity.settings, settingsEntity.type, palette, strip.length()
        )

        val activeEffect = ActiveLightEffect(
            effectUuid = effectEntity.uuid,
            layer = effectEntity.layer,
            skipFramesIfBlank = settingsEntity.skipFramesIfBlank,
            status = effectEntity.status,
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
                val settingsEntity = getSettings(it)
                GetStripEffectResponse(
                    name = it.name,
                    uuid = it.uuid,
                    stripUuid = stripEntity.uuid,
                    paletteUuid = it.palette?.uuid,
                    settingsUuid = settingsEntity.uuid,
                    status = it.status,
                    type = settingsEntity.type,
                    category = EffectCategory.forEffect(settingsEntity.type),
                    layer = it.layer,
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
                val settingsEntity = getSettings(it)
                GetPoolEffectResponse(
                    name = it.name,
                    uuid = it.uuid,
                    poolUuid = poolEntity.uuid,
                    paletteUuid = it.palette?.uuid,
                    settingsUuid = settingsEntity.uuid,
                    status = it.status,
                    type = settingsEntity.type,
                    category = EffectCategory.forEffect(settingsEntity.type),
                    layer = it.layer,
                )
            }

            GetEffectsResponse(effectList)
        } else {
            throw ClientRequestException("Could not get effects. Pool with uuid $poolUuid does not exist!")
        }
    }

    fun getAllEffects(): GetEffectsResponse {
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

    @Transactional
    open fun deleteEffect(effectUuid: String) {
        val effectEntityOptional = effectRepository.findByUuid(effectUuid)
        if (effectEntityOptional.isPresent) {
            val effectEntity = effectEntityOptional.get()
            val owningStrip = effectEntity.strip
            val owningPool = effectEntity.pool
            val deletedLayer = effectEntity.layer
            val filterIds = effectEntity.filterJunctions.map { it.filter!!.id }
            val filterEntities = filterRepository.findByIdIn(filterIds)
            val triggerEntities = triggerRepository.findByEffect(effectEntity)

            filterEntities.forEach { filterRepository.delete(it) }
            triggerEntities.forEach { triggerRepository.delete(it) }
            effectRepository.delete(effectEntity)

            shiftLayersDown(strip = owningStrip, pool = owningPool, fromLayer = deletedLayer)

            val activeEffect = effectRegistry.getEffectWithUuid(effectUuid)
            if (activeEffect != null) {
                effectRegistry.removeEffect(activeEffect)
            }
            refreshRegistryLayers(strip = owningStrip, pool = owningPool)
            sseEventEmitter.emit(LightEffectEvent.LightEffectDeleted(effectUuid))
        } else {
            throw ResourceNotFoundException("Effect with uuid $effectUuid doesn't exist!")
        }
    }

    @Transactional
    open fun updateEffect(uuid: String, updateEffectRequest: UpdateEffectRequest) {
        val effectEntityOptional = effectRepository.findByUuid(uuid)
        if (effectEntityOptional.isEmpty) {
            throw ResourceNotFoundException("Effect with uuid $uuid doesn't exist!")
        }
        var effectEntity = effectEntityOptional.get()
        val settingsEntity = getSettings(effectEntity)

        val requestIsEmpty = updateEffectRequest.name == null
                && updateEffectRequest.paletteUuid == null
                && updateEffectRequest.settingsUuid == null
                && updateEffectRequest.layer == null
                && !updateEffectRequest.unassignPalette
        if (requestIsEmpty) {
            throw ClientRequestException("Update request is empty; specify at least one field to change.")
        }

        if (updateEffectRequest.unassignPalette
            && updateEffectRequest.paletteUuid != null
            && updateEffectRequest.paletteUuid != effectEntity.palette?.uuid
        ) {
            throw ClientRequestException("Cannot unassign palette and assign a new palette in the same request.")
        }

        val owningStrip = effectEntity.strip
        val owningPool = effectEntity.pool
        val oldLayer = effectEntity.layer

        if (!updateEffectRequest.name.isNullOrBlank()) {
            effectEntity = effectEntity.copy(name = updateEffectRequest.name)
        }

        if (updateEffectRequest.unassignPalette) {
            effectEntity = effectEntity.copy(palette = null)
        } else if (updateEffectRequest.paletteUuid != null && updateEffectRequest.paletteUuid != effectEntity.palette?.uuid) {
            val paletteEntity = paletteRepository.findByUuid(updateEffectRequest.paletteUuid)
                .orElseThrow { ClientRequestException("No palette with uuid ${updateEffectRequest.paletteUuid}") }
            effectEntity = effectEntity.copy(palette = paletteEntity)
        }

        if (updateEffectRequest.settingsUuid != null && updateEffectRequest.settingsUuid != settingsEntity.uuid) {
            val settings = settingsRepository.findByUuid(updateEffectRequest.settingsUuid)
                .orElseThrow { ClientRequestException("No effect settings with uuid ${updateEffectRequest.settingsUuid}") }
            effectEntity = effectEntity.copy(effectSettings = settings)
        }

        val targetLayer = updateEffectRequest.layer ?: oldLayer
        val assigned = owningStrip != null || owningPool != null
        if (updateEffectRequest.layer != null && assigned) {
            val count = when {
                owningStrip != null -> effectRepository.findByStrip(owningStrip).size
                owningPool != null -> effectRepository.findByPool(owningPool).size
                else -> 0
            }
            val maxValid = count - 1
            if (targetLayer !in 0..maxValid) {
                val ownerDesc = owningStrip?.let { "strip ${it.uuid}" } ?: "pool ${owningPool!!.uuid}"
                throw ClientRequestException("Layer $targetLayer is out of range for $ownerDesc. Valid range is [0, $maxValid].")
            }
        }

        val needsShift = assigned && targetLayer != oldLayer

        if (needsShift) {
            // Park at sentinel -1, close the old gap, make room at the new layer, then write the final value.
            effectEntity = effectRepository.update(effectEntity.copy(layer = -1))
            shiftLayersDown(strip = owningStrip, pool = owningPool, fromLayer = oldLayer)
            shiftLayersUp(strip = owningStrip, pool = owningPool, fromLayer = targetLayer)
        }

        effectEntity = effectRepository.update(effectEntity.copy(layer = targetLayer))

        var effectModel = effectRegistry.getEffectWithUuid(uuid)
        if (effectModel != null && (owningStrip != null || owningPool != null)) {
            val (strip, palette, lightEffect) = buildEffectRuntime(effectEntity)

            val onlyPaletteChange = updateEffectRequest.paletteUuid != null
                    && !updateEffectRequest.unassignPalette
                    && updateEffectRequest.name == null
                    && updateEffectRequest.settingsUuid == null
                    && updateEffectRequest.layer == null
            if (onlyPaletteChange && palette != null) {
                // Palette swap only — keep the running effect state, just point it at the new palette.
                effectModel.effect.updatePalette(palette)
            } else {
                val updatedSettings = effectEntity.effectSettings
                effectModel = effectModel.copy(
                    effectUuid = effectEntity.uuid,
                    layer = effectEntity.layer,
                    skipFramesIfBlank = updatedSettings?.skipFramesIfBlank ?: effectModel.skipFramesIfBlank,
                    status = effectEntity.status,
                    strip = strip,
                    effect = lightEffect,
                )

                effectRegistry.addOrUpdateEffect(effectModel)
            }
        }

        refreshRegistryLayers(strip = owningStrip, pool = owningPool)

        sseEventEmitter.emit(LightEffectEvent.LightEffectUpdated(uuid))
    }

    @Transactional
    open fun reassignEffect(uuid: String, request: ReassignEffectRequest) {
        val effectEntityOptional = effectRepository.findByUuid(uuid)
        if (effectEntityOptional.isEmpty) {
            throw ResourceNotFoundException("Effect with uuid $uuid doesn't exist!")
        }

        val requestIsEmpty = !request.unassign && request.targetStripUuid == null && request.targetPoolUuid == null
        if (requestIsEmpty) {
            throw ClientRequestException("Reassign request is empty; specify 'targetStripUuid', 'targetPoolUuid', or set 'unassign' to true.")
        }

        if (request.targetStripUuid != null && request.targetPoolUuid != null) {
            throw ClientRequestException("Cannot assign effect to both a strip and a strip pool. Specify either 'targetStripUuid' or 'targetPoolUuid'.")
        }

        var effectEntity = effectEntityOptional.get()
        val oldStrip = effectEntity.strip
        val oldPool = effectEntity.pool
        val oldLayer = effectEntity.layer

        if (request.unassign) {
            if (request.targetStripUuid != null && request.targetStripUuid != oldStrip?.uuid) {
                throw ClientRequestException("Cannot unassign and reassign to a different strip in the same request.")
            }
            if (request.targetPoolUuid != null && request.targetPoolUuid != oldPool?.uuid) {
                throw ClientRequestException("Cannot unassign and reassign to a different pool in the same request.")
            }
        }

        val newStrip: LedStripEntity?
        val newPool: LedStripPoolEntity?
        when {
            request.unassign -> {
                newStrip = null
                newPool = null
            }
            request.targetStripUuid != null -> {
                newStrip = stripRepository.findByUuid(request.targetStripUuid)
                    .orElseThrow { ClientRequestException("No LED strip with uuid ${request.targetStripUuid}") }
                newPool = null
            }
            request.targetPoolUuid != null -> {
                newPool = poolRepository.findByUuid(request.targetPoolUuid)
                    .orElseThrow { ClientRequestException("No strip pool found with uuid ${request.targetPoolUuid}") }
                newStrip = null
            }
            else -> throw ClientRequestException("Reassign request is empty.")
        }

        val sameOwner = newStrip?.id == oldStrip?.id && newPool?.id == oldPool?.id
        if (sameOwner) {
            // Request specified the current owner with no other changes — nothing to do.
            sseEventEmitter.emit(LightEffectEvent.LightEffectUpdated(uuid))
            return
        }

        val unassigned = newStrip == null && newPool == null
        val targetLayer = if (unassigned) {
            0
        } else when {
            newStrip != null -> effectRepository.findByStrip(newStrip).size
            newPool != null -> effectRepository.findByPool(newPool).size
            else -> 0
        }

        // Park at sentinel -1 on the old owner so subsequent shifts don't collide.
        effectEntity = effectRepository.update(
            effectEntity.copy(strip = oldStrip, pool = oldPool, layer = -1)
        )
        shiftLayersDown(strip = oldStrip, pool = oldPool, fromLayer = oldLayer)
        shiftLayersUp(strip = newStrip, pool = newPool, fromLayer = targetLayer)

        effectEntity = effectRepository.update(
            effectEntity.copy(strip = newStrip, pool = newPool, layer = targetLayer)
        )

        val activeEffect = effectRegistry.getEffectWithUuid(uuid)
        if (unassigned) {
            // Unassigned effects cannot be rendered — drop from the registry.
            if (activeEffect != null) {
                effectRegistry.removeEffect(activeEffect)
            }
        } else {
            val (strip, _, lightEffect) = buildEffectRuntime(effectEntity)

            val updated = activeEffect?.copy(
                effectUuid = effectEntity.uuid,
                layer = effectEntity.layer,
                skipFramesIfBlank = effectEntity.effectSettings?.skipFramesIfBlank ?: activeEffect.skipFramesIfBlank,
                status = effectEntity.status,
                strip = strip,
                effect = lightEffect,
            ) ?: ActiveLightEffect(
                effectUuid = effectEntity.uuid,
                layer = effectEntity.layer,
                skipFramesIfBlank = effectEntity.effectSettings?.skipFramesIfBlank ?: true,
                status = effectEntity.status,
                strip = strip,
                effect = lightEffect,
                filters = listOf(),
            )
            effectRegistry.addOrUpdateEffect(updated)
        }

        if (oldStrip != null && oldStrip.id != newStrip?.id) refreshRegistryLayers(strip = oldStrip, pool = null)
        if (oldPool != null && oldPool.id != newPool?.id) refreshRegistryLayers(strip = null, pool = oldPool)
        refreshRegistryLayers(strip = newStrip, pool = newPool)

        sseEventEmitter.emit(LightEffectEvent.LightEffectUpdated(uuid))
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
            LightEffectStatusCommand.Deactivate -> LightEffectStatus.Inactive
            LightEffectStatusCommand.Play -> LightEffectStatus.Playing
            LightEffectStatusCommand.Pause -> LightEffectStatus.Paused
            LightEffectStatusCommand.Stop -> LightEffectStatus.Stopped
        }
    }

    fun getEffectResponseForEffect(lightEffectEntity: LightEffectEntity): GetEffectResponse? {
        val settingsEntity = getSettings(lightEffectEntity)
        return if (lightEffectEntity.strip != null) {
            GetStripEffectResponse(
                name = lightEffectEntity.name,
                uuid = lightEffectEntity.uuid,
                stripUuid = lightEffectEntity.strip!!.uuid,
                paletteUuid = lightEffectEntity.palette?.uuid,
                settingsUuid = lightEffectEntity.effectSettings?.uuid,
                status = lightEffectEntity.status,
                type = settingsEntity.type,
                category = EffectCategory.forEffect(settingsEntity.type),
                layer = lightEffectEntity.layer,
            )
        } else if (lightEffectEntity.pool != null) {
            GetPoolEffectResponse(
                name = lightEffectEntity.name,
                uuid = lightEffectEntity.uuid,
                poolUuid = lightEffectEntity.pool!!.uuid,
                paletteUuid = lightEffectEntity.palette?.uuid,
                settingsUuid = lightEffectEntity.effectSettings?.uuid,
                status = lightEffectEntity.status,
                type = settingsEntity.type,
                category = EffectCategory.forEffect(settingsEntity.type),
                layer = lightEffectEntity.layer,
            )
        } else {
            GetUnassignedEffectResponse(
                name = lightEffectEntity.name,
                uuid = lightEffectEntity.uuid,
                paletteUuid = lightEffectEntity.palette?.uuid,
                settingsUuid = lightEffectEntity.effectSettings?.uuid,
                status = lightEffectEntity.status,
                type = settingsEntity.type,
                category = EffectCategory.forEffect(settingsEntity.type),
                layer = lightEffectEntity.layer,
            )
        }
    }

    /**
     * Gets effect settings with the following logic.
     *
     * 1. [settingsUuid] is not null, fetch it from the database and throw a [ClientRequestException] if it isn't found.
     * 2. Query the database for a default settings entity matching [effectType].
     * 3. If default settings aren't found, create a settings entry in the database with default values for the effect type.
     *
     */
    private fun resolveSettingsOnCreate(
        settingsUuid: String?, effectType: String, settings: Map<String, Any>?
    ): LightEffectSettingsEntity {

        if (settingsUuid != null) {
            val settingsOptional = settingsRepository.findByUuid(settingsUuid)
            if (settingsOptional.isEmpty) {
                throw ClientRequestException("Effect settings with uuid $settingsUuid don't exist")
            }
            val entity = settingsOptional.get()
            return entity
        }

        val defaultEntity = settingsRepository.findByTypeAndIsDefault(effectType, true).orElse(null)
        if (defaultEntity != null) {
            return defaultEntity
        }

        val settingsMap = settings ?: defaultSettingsMapForType(effectType)
        val newEntity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = effectType,
                name = "Default $effectType",
                settings = settingsMap,
                isDefault = true,
            )
        )
        return newEntity
    }

    private fun defaultSettingsMapForType(effectType: String): Map<String, Any> {
        val defaults: Any = when (LightEffectType.fromName(effectType)) {
            LightEffectType.SPECTRUM -> SpectrumEffectSettings()
            LightEffectType.NIGHTRIDER_COLOR_FILL -> NightriderColorFillEffectSettings()
            LightEffectType.NIGHTRIDER_COMET -> NightriderCometEffectSettings()
            LightEffectType.FLAME -> FlameEffectSettings()
            LightEffectType.BOUNCING_BALL -> BouncingBallEffectSettings()
            LightEffectType.WAVE -> WaveEffectSettings()
            LightEffectType.MARQUEE -> MarqueeEffectSettings()
            LightEffectType.SPARKLE -> SparkleEffectSettings()
        }
        @Suppress("UNCHECKED_CAST") return objectMapper.readValue(
            objectMapper.writeValueAsString(defaults),
            Map::class.java
        ) as Map<String, Any>
    }

    fun getAllEffectSettings(): GetAllEffectSettingsResponse {
        val entities = settingsRepository.queryAll()
        return GetAllEffectSettingsResponse(entities.map { it.toResponse() })
    }

    fun getEffectSettings(uuid: String): GetEffectSettingsResponse {
        val entity = settingsRepository.findByUuid(uuid)
            .orElseThrow { ResourceNotFoundException("Effect settings with uuid $uuid not found") }
        return entity.toResponse()
    }

    fun createEffectSettings(request: CreateEffectSettingsRequest): String {
        if (request.isDefault) {
            clearExistingDefault(request.type)
        }
        val entity = settingsRepository.save(
            LightEffectSettingsEntity(
                uuid = UUID.randomUUID().toString(),
                type = request.type,
                name = request.name,
                settings = request.settings,
                isDefault = request.isDefault,
                skipFramesIfBlank = request.skipFramesIfBlank,
            )
        )
        sseEventEmitter.emit(EffectSettingsEvent.EffectSettingsCreated(entity.uuid))
        return entity.uuid
    }

    fun updateEffectSettings(uuid: String, request: UpdateEffectSettingsRequest) {
        var entity = settingsRepository.findByUuid(uuid)
            .orElseThrow { ResourceNotFoundException("Effect settings with uuid $uuid not found") }

        if (request.isDefault == true && !entity.isDefault) {
            clearExistingDefault(entity.type)
        }

        entity = entity.copy(
            name = request.name ?: entity.name,
            settings = request.settings ?: entity.settings,
            isDefault = request.isDefault ?: entity.isDefault,
            skipFramesIfBlank = request.skipFramesIfBlank ?: entity.skipFramesIfBlank,
        )
        settingsRepository.update(entity)
        sseEventEmitter.emit(EffectSettingsEvent.EffectSettingsUpdated(uuid))
    }

    fun deleteEffectSettings(uuid: String) {
        val entity = settingsRepository.findByUuid(uuid)
            .orElseThrow { ResourceNotFoundException("Effect settings with uuid $uuid not found") }
        settingsRepository.delete(entity)
        sseEventEmitter.emit(EffectSettingsEvent.EffectSettingsDeleted(uuid))
    }

    private fun clearExistingDefault(type: String) {
        val existing = settingsRepository.findByTypeAndIsDefault(type, true).orElse(null)
        if (existing != null) {
            settingsRepository.update(existing.copy(isDefault = false))
        }
    }

    private fun validateLayerForInsert(targetLayer: Int, count: Int, ownerDesc: String) {
        if (targetLayer !in 0..count) {
            throw ClientRequestException(
                "Layer $targetLayer is out of range for $ownerDesc. Valid range is [0, $count]."
            )
        }
    }

    /**
     * Increments the layer of every effect on [strip]/[pool] with layer >= [fromLayer] by 1.
     * Iterates in descending order of layer so each individual UPDATE writes into a free slot, avoiding
     * the unique-per-(owner, layer) constraint collisions that a single bulk UPDATE would trigger.
     */
    private fun shiftLayersUp(strip: LedStripEntity?, pool: LedStripPoolEntity?, fromLayer: Int) {
        val effects = when {
            strip != null -> effectRepository.findByStrip(strip)
            pool != null -> effectRepository.findByPool(pool)
            else -> return
        }
        effects.filter { it.layer >= fromLayer }.sortedByDescending { it.layer }.forEach {
            effectRepository.update(it.copy(layer = it.layer + 1))
        }
    }

    /** Decrements the layer of every effect on [strip]/[pool] with layer > [fromLayer] by 1, in ascending order. */
    private fun shiftLayersDown(strip: LedStripEntity?, pool: LedStripPoolEntity?, fromLayer: Int) {
        val effects = when {
            strip != null -> effectRepository.findByStrip(strip)
            pool != null -> effectRepository.findByPool(pool)
            else -> return
        }
        effects.filter { it.layer > fromLayer }.sortedBy { it.layer }.forEach {
            effectRepository.update(it.copy(layer = it.layer - 1))
        }
    }

    private data class EffectRuntime(
        val strip: LedStripModel,
        val palette: ColorPalette?,
        val lightEffect: LightEffect,
    )

    /** Rebuilds the renderable strip/palette/effect trio from a persisted effect entity. */
    private fun buildEffectRuntime(effectEntity: LightEffectEntity): EffectRuntime {
        val strip = createLightingService.ledStripFromEffectEntity(effectEntity)
        val palette = effectEntity.palette?.let {
            createLightingService.createPalette(it.settings, it.type, it.uuid, strip.length())
        }
        val settings = effectEntity.effectSettings?.settings ?: emptyMap()
        val type = effectEntity.effectSettings?.type ?: ""
        val lightEffect = createLightingService.createEffect(settings, type, palette, strip.length())
        return EffectRuntime(strip, palette, lightEffect)
    }

    private fun refreshRegistryLayers(strip: LedStripEntity?, pool: LedStripPoolEntity?) {
        val effects = when {
            strip != null -> effectRepository.findByStrip(strip)
            pool != null -> effectRepository.findByPool(pool)
            else -> return
        }
        for (effect in effects) {
            val active = effectRegistry.getEffectWithUuid(effect.uuid)
            if (active != null && active.layer != effect.layer) {
                effectRegistry.addOrUpdateEffect(active.copy(layer = effect.layer))
            }
        }
    }

    private fun getSettings(entity: LightEffectEntity): LightEffectSettingsEntity {
        // Missing settings shouldn't be possible due to SQL constraints, if this happens something went very wrong.
        return entity.effectSettings
            ?: throw ClientRequestException("Effect ${entity.uuid} does not have settings!")
    }

    private fun LightEffectSettingsEntity.toResponse() = GetEffectSettingsResponse(
        uuid = uuid,
        type = type,
        name = name,
        settings = settings,
        isDefault = isDefault,
        skipFramesIfBlank = skipFramesIfBlank,
    )

    fun getAllSchemas(): List<EffectSettingsSchema> = LightEffectType.entries.map { effectType ->
        when (effectType) {
            LightEffectType.SPECTRUM -> EffectSettingsSchemaBuilder(effectType.displayName).integer(
                "colorBandPercentage",
                "Width of each color band as a percentage of strip length"
            ) { min(1.0); default(10) }
                .boolean("animated", "Whether the spectrum cycles through colors over time", { default(true) })
                .integer("updatesPerSecond", "Number of animation steps per second") { min(1.0); default(30) }.build()

            LightEffectType.NIGHTRIDER_COLOR_FILL -> EffectSettingsSchemaBuilder(effectType.displayName).boolean(
                "wrap",
                "Whether the fill wraps around the strip ends",
                { default(false) }
            ).integer("updatesPerSecond", "Number of position updates per second") { min(1.0); default(35) }
                .number(
                    "brightnessScaling",
                    "Brightness multiplier applied to the effect"
                ) { min(0.0); max(1.0); default(0.2f) }
                .build()

            LightEffectType.NIGHTRIDER_COMET -> EffectSettingsSchemaBuilder(effectType.displayName).integer(
                "trailLength",
                "Number of pixels in the comet's trailing tail"
            ) { min(1.0); default(5) }.string("trailFadeCurve", "Brightness falloff curve along the trail") {
                options(FadeCurve.entries.map { it.name }); default(FadeCurve.Linear.name)
            }.boolean("wrap", "Whether the comet wraps around the strip ends", { default(false) })
                .integer("updatesPerSecond", "Number of position updates per second") { min(1.0); default(35) }.build()

            LightEffectType.FLAME -> EffectSettingsSchemaBuilder(effectType.displayName).integer(
                "cooling",
                "Rate at which heat dissipates up the strip"
            ) { min(1.0); default(11) }.integer(
                "sparking", "Probability of new sparks igniting at the base (0–255)"
            ) { min(0.0); max(255.0); default(140) }
                .integer("sparks", "Number of sparks generated per update") { min(1.0); default(1) }
                .integer("sparkHeight", "Maximum height sparks can reach from the base") { min(1.0); default(3) }
                .integer("updatesPerSecond", "Number of fire simulation steps per second") { min(1.0); default(30) }
                .build()

            LightEffectType.BOUNCING_BALL -> EffectSettingsSchemaBuilder(effectType.displayName).integer(
                "startingHeightPercent", "Initial drop height as a percentage of strip length"
            ) { min(0.0); max(100.0); default(1) }
                .integer("maxHeightPercent", "Maximum bounce height as a percentage of strip length") { min(1.0); max(100.0); default(100) }
                .number("speed", "Initial speed of the ball") { min(0.0); default(4.0) }
                .number(
                    "gravity",
                    "Gravitational acceleration applied to the ball"
                ) { default(LightEffectConstants.EARTH_GRAVITY) }
                .number("minimumSpeed", "Speed below which the ball stops bouncing") { min(0.0); default(0.05) }.build()

            LightEffectType.WAVE -> EffectSettingsSchemaBuilder(effectType.displayName).integer(
                "startPointPercentage",
                "Starting pixel position of the wave as a percentage of the strip length"
            ) { min(0.0); default(50) }
                .integer("waveLength", "Length of one full wave cycle in pixels") { min(1.0); default(10) }
                .boolean("repeat", "Whether the wave repeats continuously") { default(false) }
                .integer("updatesPerSecond", "Number of wave position steps per second") { min(1.0); default(30) }
                .build()

            LightEffectType.MARQUEE -> EffectSettingsSchemaBuilder(effectType.displayName).integer(
                "dotLength",
                "Length of each dot in pixels"
            ) { min(1.0); default(2) }
                .integer("spaceBetweenDots", "Gap between dots in pixels") { min(0.0); default(2) }
                .integer("updatesPerSecond", "Number of pixels the dots scroll per second") { min(1.0); default(8) }
                .build()

            LightEffectType.SPARKLE -> EffectSettingsSchemaBuilder(effectType.displayName).integer(
                "numDots",
                "Maximum number of simultaneous sparkle dots"
            ) { min(1.0); default(10) }
                .integer("fadeInMillisMax", "Maximum fade-in duration in milliseconds") { min(1.0); default(10) }
                .integer("fadeInMillisMin", "Minimum fade-in duration in milliseconds") { min(1.0); default(5) }
                .integer("fadeOutMillisMax", "Maximum fade-out duration in milliseconds") { min(1.0); default(1000) }
                .integer("fadeOutMillisMin", "Minimum fade-out duration in milliseconds") { min(1.0); default(150) }
                .integer("updatesPerSecond", "Number of sparkle state updates per second") { min(1.0); default(30) }
                .build()
        }
    }
}
