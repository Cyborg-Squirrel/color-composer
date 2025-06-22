package io.cyborgsquirrel.lighting.effect_palette.controller

import io.cyborgsquirrel.lighting.effect_palette.api.PaletteApi
import io.cyborgsquirrel.lighting.effect_palette.requests.CreatePaletteRequest
import io.cyborgsquirrel.lighting.effect_palette.requests.UpdatePaletteRequest
import io.cyborgsquirrel.lighting.effect_palette.service.PaletteApiService
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller

@Controller("/palette")
class PaletteController(private val apiService: PaletteApiService) : PaletteApi {
    override fun getAllPalettes(): HttpResponse<Any> {
        return try {
            val palettes = apiService.getAllPalettes()
            HttpResponse.ok(palettes)
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun getPalette(uuid: String): HttpResponse<Any> {
        return try {
            val palette = apiService.getPalette(uuid)
            HttpResponse.ok(palette)
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun createPalette(request: CreatePaletteRequest): HttpResponse<Any> {
        return try {
            val uuid = apiService.createPalette(request)
            return HttpResponse.created(uuid)
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun updatePalette(uuid: String, request: UpdatePaletteRequest): HttpResponse<Any> {
        return try {
            apiService.updatePalette(request, uuid)
            HttpResponse.noContent()
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun deletePalette(uuid: String): HttpResponse<Any> {
        return try {
            apiService.deletePalette(uuid)
            HttpResponse.ok()
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }
}