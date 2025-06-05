package io.cyborgsquirrel.lighting.filters.api

import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*

interface EffectFilterApi {

    @Get
    fun getAllEffectFilters(): HttpResponse<Any>

    @Get
    fun getFiltersForEffect(@QueryValue effectUuid: String): HttpResponse<Any>

    @Get("/{uuid}")
    fun getEffectFilter(uuid: String) : HttpResponse<Any>

    @Post
    fun createEffectFilter(@Body request: CreateEffectRequest): HttpResponse<Any>

    @Patch("/{uuid}")
    fun updateEffectFilter(uuid: String, @Body request: UpdateEffectRequest): HttpResponse<Any>

    @Delete("/{uuid}")
    fun deleteEffectFilter(uuid: String): HttpResponse<Any>
}