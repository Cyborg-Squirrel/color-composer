package io.cyborgsquirrel.lighting.filters.service

import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.service.CreateLightingHelper
import io.cyborgsquirrel.lighting.filters.entity.LightEffectFilterEntity
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
    private val effectRegistry: ActiveLightEffectRegistry,
    private val effectLightingHelper: CreateLightingHelper,
) {

    fun getFiltersForEffect(effectUuid: String): GetFiltersResponse {
        val effectOptional = effectRepository.findByUuid(effectUuid)
        if (effectOptional.isPresent) {
            val filterEntities = effectOptional.get().filters
            val filterResponses = filterEntities.map { filter ->
                GetFilterResponse(filter.name!!, filter.type!!, filter.uuid!!, filter.effect?.uuid, filter.settings!!)
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
            return GetFilterResponse(
                filter.name!!,
                filter.type!!,
                filter.uuid!!,
                filter.effect?.uuid,
                filter.settings!!
            )
        } else {
            throw ClientRequestException("Filter with uuid $uuid does not exist")
        }
    }

    fun createFilter(request: CreateEffectFilterRequest): String {
        val effectEntity =
            if (request.effectUuid == null) {
                null
            } else {
                val effectOptional = effectRepository.findByUuid(request.effectUuid)
                if (effectOptional.isEmpty) {
                    throw ClientRequestException("No effect found with uuid ${request.effectUuid}")
                }
                effectOptional.get()
            }

        val filter =
            effectLightingHelper.createEffectFilter(request.filterType, UUID.randomUUID().toString(), request.settings)
        val filterEntity = LightEffectFilterEntity(
            effect = effectEntity,
            uuid = filter.uuid,
            settings = request.settings,
            name = request.name,
            type = request.filterType,
        )

        filterRepository.save(filterEntity)

        if (effectEntity != null) {
            val activeEffectOptional = effectRegistry.getEffectWithUuid(request.effectUuid!!)
            val activeEffect = activeEffectOptional.get()
            effectRegistry.addOrUpdateEffect(activeEffect.copy(filters = activeEffect.filters + filter))
        }

        return filterEntity.uuid!!
    }

    fun updateFilter(uuid: String, request: UpdateEffectFilterRequest) {
        val filterEntityOptional = filterRepository.findByUuid(uuid)
        if (filterEntityOptional.isPresent) {
            var filterEntity = filterEntityOptional.get()
            val activeEffect = if (filterEntity.effect != null) {
                val activeEffectOptional = effectRegistry.getEffectWithUuid(filterEntity.effect!!.uuid!!)
                if (activeEffectOptional.isPresent) {
                    activeEffectOptional.get()
                } else {
                    throw ClientRequestException("No effect found with uuid ${request.effectUuid}")
                }
            } else if (request.effectUuid != null) {
                val activeEffectOptional = effectRegistry.getEffectWithUuid(request.effectUuid)
                if (activeEffectOptional.isPresent) {
                    activeEffectOptional.get()
                } else {
                    throw ClientRequestException("No effect found with uuid ${request.effectUuid}")
                }
            } else null

            if (request.name != null) {
                filterEntity = filterEntity.copy(name = request.name)
            }

            if (request.effectUuid != filterEntity.effect?.uuid) {
                val requestEffectEntity = if (request.effectUuid != null) {
                    val requestEffectEntityOptional = effectRepository.findByUuid(request.effectUuid)
                    if (requestEffectEntityOptional.isPresent) {
                        requestEffectEntityOptional.get()
                    } else {
                        throw ClientRequestException("No effect found with uuid ${request.effectUuid}")
                    }
                } else null

                filterEntity.effect = requestEffectEntity
            }

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

            filterRepository.update(filterEntity)

            if (activeEffect != null) {
                val filterList = activeEffect.filters.filter { it.uuid != filterEntity.uuid } + activeFilter
                effectRegistry.addOrUpdateEffect(activeEffect.copy(filters = filterList))
            }
        } else {
            throw ClientRequestException("No filter found with uuid $uuid")
        }
    }

    fun deleteFilter(uuid: String) {
        val filterOptional = filterRepository.findByUuid(uuid)
        if (filterOptional.isPresent) {
            val filter = filterOptional.get()
            val activeEffectOptional = effectRegistry.getEffectWithUuid(filter.effect!!.uuid!!)
            if (activeEffectOptional.isPresent) {
                val activeEffect = activeEffectOptional.get()
                val filters = activeEffect.filters.filter { it.uuid != uuid }
                effectRegistry.addOrUpdateEffect(activeEffect.copy(filters = filters))
            }

            filterRepository.delete(filter)
        } else {
            throw ClientRequestException("No filter found with uuid $uuid")
        }
    }
}