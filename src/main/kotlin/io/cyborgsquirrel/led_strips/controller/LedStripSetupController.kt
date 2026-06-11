package io.cyborgsquirrel.led_strips.controller

import io.cyborgsquirrel.led_strips.api.LedStripSetupApi
import io.cyborgsquirrel.led_strips.requests.CreateLedStripRequest
import io.cyborgsquirrel.led_strips.requests.UpdateLedStripRequest
import io.cyborgsquirrel.led_strips.services.LedStripApiService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.QueryValue

@Controller("/strip")
class LedStripSetupController(
    private val stripSetupService: LedStripApiService,
) : LedStripSetupApi {

    override fun getStrips(@QueryValue clientUuid: String?): HttpResponse<Any> {
        return HttpResponse.ok(stripSetupService.getStrips(clientUuid))
    }

    override fun getStrip(uuid: String): HttpResponse<Any> {
        return HttpResponse.ok(stripSetupService.getStrip(uuid))
    }

    override fun createStrip(@Body request: CreateLedStripRequest): HttpResponse<Any> {
        return HttpResponse.created(stripSetupService.createStrip(request))
    }

    override fun updateStrip(uuid: String, @Body updatedStrip: UpdateLedStripRequest): HttpResponse<Any> {
        stripSetupService.updateStrip(uuid, updatedStrip)
        return HttpResponse.noContent()
    }

    override fun deleteStrip(uuid: String): HttpResponse<Any> {
        stripSetupService.onStripDeleted(uuid)
        return HttpResponse.noContent()
    }
}
