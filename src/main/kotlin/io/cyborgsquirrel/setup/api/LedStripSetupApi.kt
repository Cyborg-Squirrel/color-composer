package io.cyborgsquirrel.setup.api

import io.cyborgsquirrel.setup.requests.strip.CreateLedStripRequest
import io.cyborgsquirrel.setup.requests.strip.UpdateLedStripRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*

interface LedStripSetupApi {
    @Get
    fun getStripsForClient(@QueryValue clientUuid: String): HttpResponse<Any>

    @Get("/{uuid}")
    fun getStrip(uuid: String): HttpResponse<Any>

    @Put
    fun createStrip(@QueryValue clientUuid: String, @Body request: CreateLedStripRequest): HttpResponse<Any>

    @Patch("/{uuid}")
    fun updateStrip(uuid: String, @Body request: UpdateLedStripRequest): HttpResponse<Any>
}