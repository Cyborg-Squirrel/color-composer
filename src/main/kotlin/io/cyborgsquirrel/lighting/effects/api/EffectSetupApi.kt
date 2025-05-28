package io.cyborgsquirrel.lighting.effects.api

import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectRequest
import io.cyborgsquirrel.lighting.effects.responses.GetEffectResponse
import io.cyborgsquirrel.lighting.effects.responses.GetEffectsResponse
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*

interface EffectSetupApi {

    @Get
    fun getAllEffects(): HttpResponse<GetEffectsResponse>

    @Get
    fun getEffectsForStrip(@QueryValue stripUuid: String?, @QueryValue groupUuid: String?): HttpResponse<Any>

    @Get("/{uuid}")
    fun getEffect(uuid: String) : HttpResponse<Any>

    @Post
    fun createEffect(@Body request: CreateEffectRequest): HttpResponse<Any>

    @Patch("/{uuid}")
    fun updateEffect(uuid: String, @Body request: UpdateEffectRequest): HttpResponse<Any>

    @Delete("/{uuid}")
    fun deleteEffect(uuid: String): HttpResponse<Any>
}