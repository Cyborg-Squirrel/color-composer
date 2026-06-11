package io.cyborgsquirrel.lighting.effect_palette.controller

import io.cyborgsquirrel.lighting.effect_palette.api.PaletteApi
import io.cyborgsquirrel.lighting.effect_palette.requests.CreatePaletteRequest
import io.cyborgsquirrel.lighting.effect_palette.requests.UpdatePaletteRequest
import io.cyborgsquirrel.lighting.effect_palette.service.PaletteApiService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller

@Controller("/palette")
class PaletteController(private val apiService: PaletteApiService) : PaletteApi {
    override fun getAllPalettes(): HttpResponse<Any> {
        return HttpResponse.ok(apiService.getAllPalettes())
    }

    override fun getPalette(uuid: String): HttpResponse<Any> {
        return HttpResponse.ok(apiService.getPalette(uuid))
    }

    override fun createPalette(request: CreatePaletteRequest): HttpResponse<Any> {
        return HttpResponse.created(apiService.createPalette(request))
    }

    override fun updatePalette(uuid: String, request: UpdatePaletteRequest): HttpResponse<Any> {
        apiService.updatePalette(request, uuid)
        return HttpResponse.noContent()
    }

    override fun deletePalette(uuid: String): HttpResponse<Any> {
        apiService.deletePalette(uuid)
        return HttpResponse.noContent()
    }
}
