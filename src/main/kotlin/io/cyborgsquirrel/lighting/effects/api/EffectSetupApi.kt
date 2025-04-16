package io.cyborgsquirrel.lighting.effects.api

import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*

interface EffectSetupApi {

    @Get("/all")
    fun getAllEffects(): HttpResponse<Any>

    @Get
    fun getEffectsForStrip(@QueryValue stripUuid: String?, @QueryValue groupUuid: String?): HttpResponse<Any>

    @Post
    fun createEffect(
        @QueryValue stripUuid: String,
        @Body request: CreateEffectRequest
    ): HttpResponse<Any>

    @Patch("/{uuid}")
    fun updateEffect(uuid: String): HttpResponse<Any>

    @Delete("/{uuid}")
    fun deleteEffect(uuid: String): HttpResponse<Any>
}