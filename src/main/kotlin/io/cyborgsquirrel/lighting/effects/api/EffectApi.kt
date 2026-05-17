package io.cyborgsquirrel.lighting.effects.api

import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectSettingsRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectSettingsRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectStatusRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*

interface EffectApi {

    @Get
    fun getEffects(@QueryValue stripUuid: String?, @QueryValue poolUuid: String?): HttpResponse<Any>

    @Get("/schemas")
    fun getSchemas(): HttpResponse<Any>

    @Get("/{uuid}")
    fun getEffect(uuid: String) : HttpResponse<Any>

    @Post
    fun createEffect(@Body request: CreateEffectRequest): HttpResponse<Any>

    @Patch("/{uuid}")
    fun updateEffect(uuid: String, @Body request: UpdateEffectRequest): HttpResponse<Any>

    @Post("/status")
    fun updateEffectStatuses(@Body request: UpdateEffectStatusRequest) : HttpResponse<Any>

    @Delete("/{uuid}")
    fun deleteEffect(uuid: String): HttpResponse<Any>

    @Get("/settings")
    fun getAllEffectSettings(): HttpResponse<Any>

    @Get("/settings/{uuid}")
    fun getEffectSettings(uuid: String): HttpResponse<Any>

    @Post("/settings")
    fun createEffectSettings(@Body request: CreateEffectSettingsRequest): HttpResponse<Any>

    @Patch("/settings/{uuid}")
    fun updateEffectSettings(uuid: String, @Body request: UpdateEffectSettingsRequest): HttpResponse<Any>

    @Delete("/settings/{uuid}")
    fun deleteEffectSettings(uuid: String): HttpResponse<Any>
}