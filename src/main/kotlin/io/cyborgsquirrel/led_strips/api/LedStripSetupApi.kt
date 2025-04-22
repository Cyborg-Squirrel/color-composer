package io.cyborgsquirrel.led_strips.api

import io.cyborgsquirrel.led_strips.requests.CreateLedStripRequest
import io.cyborgsquirrel.led_strips.requests.UpdateLedStripRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*

interface LedStripSetupApi {
    @Get
    fun getStripsForClient(@QueryValue clientUuid: String): HttpResponse<Any>

    @Get("/{uuid}")
    fun getStrip(uuid: String): HttpResponse<Any>

    @Put
    fun createStrip(@Body request: CreateLedStripRequest): HttpResponse<Any>

    @Patch("/{uuid}")
    fun updateStrip(uuid: String, @Body updatedStrip: UpdateLedStripRequest): HttpResponse<Any>

    @Delete("/{uuid}")
    fun deleteStrip(uuid: String): HttpResponse<Any>
}