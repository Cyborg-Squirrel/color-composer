package io.cyborgsquirrel.lighting.filters.controller

import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectRequest
import io.cyborgsquirrel.lighting.filters.api.EffectFilterApi
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller

@Controller("/filter")
class EffectFilterController : EffectFilterApi {
    override fun getAllEffectFilters(): HttpResponse<Any> {
        TODO("Not yet implemented")
    }

    override fun getFiltersForEffect(effectUuid: String): HttpResponse<Any> {
        TODO("Not yet implemented")
    }

    override fun getEffectFilter(uuid: String): HttpResponse<Any> {
        TODO("Not yet implemented")
    }

    override fun createEffectFilter(request: CreateEffectRequest): HttpResponse<Any> {
        TODO("Not yet implemented")
    }

    override fun updateEffectFilter(uuid: String, request: UpdateEffectRequest): HttpResponse<Any> {
        TODO("Not yet implemented")
    }

    override fun deleteEffectFilter(uuid: String): HttpResponse<Any> {
        TODO("Not yet implemented")
    }
}