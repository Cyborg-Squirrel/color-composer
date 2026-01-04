package io.cyborgsquirrel.lighting.filters.service

import io.cyborgsquirrel.lighting.effects.service.ActiveLightEffectService
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.service.CreateLightingService
import io.cyborgsquirrel.lighting.filters.entity.LightEffectFilterEntity
import io.cyborgsquirrel.lighting.filters.entity.LightEffectFilterJunctionEntity
import io.cyborgsquirrel.lighting.filters.repository.H2LightEffectFilterJunctionRepository
import io.cyborgsquirrel.lighting.filters.repository.H2LightEffectFilterRepository
import io.cyborgsquirrel.lighting.filters.requests.CreateEffectFilterRequest
import io.cyborgsquirrel.lighting.filters.requests.UpdateEffectFilterRequest
import io.cyborgsquirrel.lighting.filters.responses.GetFilterResponse
import io.cyborgsquirrel.lighting.filters.responses.GetFiltersResponse
import io.cyborgsquirrel.util.exception.ClientRequestException
import jakarta.inject.Singleton
import java.util.*

@Singleton
class EffectFilterApiService(
    private val effectRepository: H2LightEffectRepository,
    private val filterRepository: H2LightEffectFilterRepository,
    private val junctionRepository: H2LightEffectFilterJunctionRepository,
    private val effectRegistry: ActiveLightEffectService,
    private val effectLightingHelper: CreateLightingService,
) {

    fun getFiltersForEffect(effectUuid: String): GetFiltersResponse {
        val effectOptional = effectRepository.findByUuid(effectUuid)
        if (effectOptional.isPresent) {
            val effectEntity = effectOptional.get()
            val junctionEntities = junctionRepository.findByEffect(effectEntity)
            val filterEntities = filterRepository.findByIdIn(junctionEntities.map { it.filter!!.id })
            val filterResponses = filterEntities.map { filter ->
                GetFilterResponse(
                    filter.name!!,
                    filter.type!!,
                    filter.uuid!!,
                    listOf(effectEntity.uuid!!),
                    filter.settings!!
                )
            }
            return GetFiltersResponse(filterResponses)
        } else {
            throw ClientRequestException("Effect with uuid $effectUuid does not exist")
        }
    }

    fun getFilter(uuid: String): GetFilterResponse {
        val filterOptional = filterRepository.findByUuid(uuid)
        if (filterOptional.isPresent) {
            val filter = filterOptional.get()
            val effectIds = filter.effectJunctions.map { it.effect!!.id }
            val effectEntities = effectRepository.findByIdIn(effectIds)
            return GetFilterResponse(
                filter.name!!,
                filter.type!!,
                filter.uuid!!,
                effectEntities.map { it.uuid!! },
                filter.settings!!
            )
        } else {
            throw ClientRequestException("Filter with uuid $uuid does not exist")
        }
    }

    fun createFilter(request: CreateEffectFilterRequest): String {
        val filter =
            effectLightingHelper.createEffectFilter(request.filterType, UUID.randomUUID().toString(), request.settings)
        var filterEntity = LightEffectFilterEntity(
            uuid = filter.uuid,
            settings = request.settings,
            name = request.name,
            type = request.filterType,
        )

        filterEntity = filterRepository.save(filterEntity)
        return filterEntity.uuid!!
    }

    fun updateFilter(uuid: String, request: UpdateEffectFilterRequest) {
        val filterEntityOptional = filterRepository.findByUuid(uuid)
        if (filterEntityOptional.isPresent) {
            var filterEntity = filterEntityOptional.get()

            filterEntity = filterEntity.copy(
                name = request.name ?: filterEntity.name,
                settings = request.settings ?: filterEntity.settings
            )

            val activeFilter = if (request.settings != null) {
                filterEntity = filterEntity.copy(settings = request.settings)
                effectLightingHelper.createEffectFilter(
                    filterEntity.type!!,
                    filterEntity.uuid!!,
                    settings = request.settings
                )
            } else {
                effectLightingHelper.createEffectFilter(
                    filterEntity.type!!,
                    filterEntity.uuid!!,
                    settings = filterEntity.settings!!
                )
            }

            filterEntity = filterRepository.update(filterEntity)

            val junctionEntities = junctionRepository.findByFilter(filterEntity)
            val effectEntities = junctionEntities.mapNotNull { it.effect }
            val currentEffectUuids = if (effectEntities.isEmpty()) listOf() else effectEntities.map { it.uuid!! }
            val junctionsToRemove = mutableSetOf<LightEffectFilterJunctionEntity>()
            val junctionsToAdd = mutableSetOf<LightEffectFilterJunctionEntity>()
            val junctionsToUpdate = mutableSetOf<LightEffectFilterJunctionEntity>()

            if (request.effectUuids != currentEffectUuids) {
                val allEffectsUuidList = currentEffectUuids.toSet() + request.effectUuids.toSet()
                for (effectUuid in allEffectsUuidList) {
                    if (!request.effectUuids.contains(effectUuid)) {
                        val junctionEntity = junctionEntities.first { it.effect!!.uuid == effectUuid }
                        junctionsToRemove.add(junctionEntity)
                    } else if (!currentEffectUuids.contains(effectUuid)) {
                        val effectEntityOptional = effectRepository.findByUuid(effectUuid)
                        if (effectEntityOptional.isPresent) {
                            val effectEntity = effectEntityOptional.get()
                            junctionsToAdd.add(
                                LightEffectFilterJunctionEntity(
                                    filter = filterEntity,
                                    effect = effectEntity
                                )
                            )
                        } else {
                            throw ClientRequestException("Effect with uuid $effectUuid does not exist!")
                        }
                    } else {
                        val junctionEntity = junctionEntities.first { it.effect!!.uuid == effectUuid }
                        junctionsToUpdate.add(junctionEntity)
                    }
                }
            }

            for (junction in junctionsToRemove) {
                junctionRepository.delete(junction)
                val activeEffectOptional = effectRegistry.getEffectWithUuid(junction.effect!!.uuid!!)
                if (activeEffectOptional.isPresent) {
                    val activeEffect = activeEffectOptional.get()
                    effectRegistry.addOrUpdateEffect(activeEffect.copy(filters = activeEffect.filters.filter { it.uuid != activeFilter.uuid }))
                }
            }

            for (junction in junctionsToAdd) {
                junctionRepository.save(junction)
                val activeEffectOptional = effectRegistry.getEffectWithUuid(junction.effect!!.uuid!!)
                if (activeEffectOptional.isPresent) {
                    val activeEffect = activeEffectOptional.get()
                    effectRegistry.addOrUpdateEffect(activeEffect.copy(filters = activeEffect.filters + activeFilter))
                }
            }

            for (junction in junctionsToUpdate) {
                val activeEffectOptional = effectRegistry.getEffectWithUuid(junction.effect!!.uuid!!)
                if (activeEffectOptional.isPresent) {
                    val activeEffect = activeEffectOptional.get()
                    val newFilters = activeEffect.filters.filter { it.uuid != activeFilter.uuid } + activeFilter
                    effectRegistry.addOrUpdateEffect(activeEffect.copy(filters = newFilters))
                }
            }
        } else {
            throw ClientRequestException("No filter found with uuid $uuid")
        }
    }

    fun deleteFilter(uuid: String) {
        val filterOptional = filterRepository.findByUuid(uuid)
        if (filterOptional.isPresent) {
            val filterEntity = filterOptional.get()
            val junctionEntities = junctionRepository.findByFilter(filterEntity)

            for (junction in junctionEntities) {
                val effect = junction.effect
                val activeEffectOptional = effectRegistry.getEffectWithUuid(effect!!.uuid!!)
                if (activeEffectOptional.isPresent) {
                    val activeEffect = activeEffectOptional.get()
                    val filters = activeEffect.filters.filter { it.uuid != uuid }
                    effectRegistry.addOrUpdateEffect(activeEffect.copy(filters = filters))
                }

                junctionRepository.delete(junction)
            }

            filterRepository.delete(filterEntity)
        } else {
            throw ClientRequestException("No filter found with uuid $uuid")
        }
    }
}