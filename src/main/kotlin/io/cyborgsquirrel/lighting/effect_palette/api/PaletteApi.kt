package io.cyborgsquirrel.lighting.effect_palette.api

import io.cyborgsquirrel.lighting.effect_palette.requests.CreatePaletteRequest
import io.cyborgsquirrel.lighting.effect_palette.requests.UpdatePaletteRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*

interface PaletteApi {
    @Get
    fun getAllPalettes(): HttpResponse<Any>

    @Get("/{uuid}")
    fun getPalette(uuid: String): HttpResponse<Any>

    @Post
    fun createPalette(@Body request: CreatePaletteRequest): HttpResponse<Any>

    @Patch("/{uuid}")
    fun updatePalette(uuid: String, @Body request: UpdatePaletteRequest): HttpResponse<Any>

    @Delete("/{uuid}")
    fun deletePalette(uuid: String): HttpResponse<Any>
}