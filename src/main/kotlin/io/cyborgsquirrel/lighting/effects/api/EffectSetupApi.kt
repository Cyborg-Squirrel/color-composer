package io.cyborgsquirrel.lighting.effects.api

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*

interface EffectSetupApi {

    @Get("/all")
    fun getAllEffects(): HttpResponse<Any>

    @Get
    fun getEffectsForStrip(@QueryValue stripUuid: String): HttpResponse<Any>

    @Put
    fun createEffect(@QueryValue stripUuid: String): HttpResponse<Any>

    @Patch("/{uuid}")
    fun updateEffect(uuid: String): HttpResponse<Any>

    @Delete("/{uuid}")
    fun deleteEffect(uuid: String): HttpResponse<Any>
}