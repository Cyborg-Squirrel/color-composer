package io.cyborgsquirrel.led_strips.controller

import io.cyborgsquirrel.led_strips.api.LedStripSetupApi
import io.cyborgsquirrel.led_strips.requests.CreateLedStripRequest
import io.cyborgsquirrel.led_strips.requests.UpdateLedStripRequest
import io.cyborgsquirrel.led_strips.service.LedStripApiService
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.QueryValue

@Controller("/strip")
class LedStripSetupController(
    private val stripSetupService: LedStripApiService,
) : LedStripSetupApi {

    override fun getStrips(@QueryValue clientUuid: String?): HttpResponse<Any> {
        return try {
            val strips = stripSetupService.getStrips(clientUuid)
            HttpResponse.ok(strips)
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun getStrip(uuid: String): HttpResponse<Any> {
        return try {
            val strip = stripSetupService.getStrip(uuid)
            HttpResponse.ok(strip)
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun createStrip(@Body request: CreateLedStripRequest): HttpResponse<Any> {
        return try {
            val stripUuid = stripSetupService.createStrip(request)
            return HttpResponse.created(stripUuid)
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun updateStrip(uuid: String, @Body updatedStrip: UpdateLedStripRequest): HttpResponse<Any> {
        return try {
            stripSetupService.updateStrip(uuid, updatedStrip)
            HttpResponse.noContent()
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun deleteStrip(uuid: String): HttpResponse<Any> {
        return try {
            stripSetupService.onStripDeleted(uuid)
            HttpResponse.noContent()
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }
}