package io.cyborgsquirrel.lighting.filters.controller

import io.cyborgsquirrel.lighting.filters.api.EffectFilterApi
import io.cyborgsquirrel.lighting.filters.requests.CreateEffectFilterRequest
import io.cyborgsquirrel.lighting.filters.requests.UpdateEffectFilterRequest
import io.cyborgsquirrel.lighting.filters.service.EffectFilterApiService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller

@Controller("/filter")
class EffectFilterController(private val filterApiService: EffectFilterApiService) : EffectFilterApi {
    override fun getAllEffectFilters(): HttpResponse<Any> {
        return HttpResponse.ok(filterApiService.getAllFilters())
    }

    override fun getFiltersForEffect(effectUuid: String): HttpResponse<Any> {
        return HttpResponse.ok(filterApiService.getFiltersForEffect(effectUuid))
    }

    override fun getEffectFilter(uuid: String): HttpResponse<Any> {
        return HttpResponse.ok(filterApiService.getFilter(uuid))
    }

    override fun createEffectFilter(request: CreateEffectFilterRequest): HttpResponse<Any> {
        return HttpResponse.created(filterApiService.createFilter(request))
    }

    override fun updateEffectFilter(uuid: String, request: UpdateEffectFilterRequest): HttpResponse<Any> {
        filterApiService.updateFilter(uuid, request)
        return HttpResponse.noContent()
    }

    override fun deleteEffectFilter(uuid: String): HttpResponse<Any> {
        filterApiService.deleteFilter(uuid)
        return HttpResponse.noContent()
    }
}
