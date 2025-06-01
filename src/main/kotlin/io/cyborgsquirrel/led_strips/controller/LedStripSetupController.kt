package io.cyborgsquirrel.led_strips.controller

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.led_strips.api.LedStripSetupApi
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.led_strips.requests.CreateLedStripRequest
import io.cyborgsquirrel.led_strips.requests.UpdateLedStripRequest
import io.cyborgsquirrel.led_strips.responses.GetLedStripResponse
import io.cyborgsquirrel.led_strips.responses.GetLedStripsResponse
import io.cyborgsquirrel.led_strips.service.LedStripSetupService
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.QueryValue
import java.util.*

@Controller("/strip")
class LedStripSetupController(
    private val clientRepository: H2LedStripClientRepository,
    private val stripRepository: H2LedStripRepository,
    private val stripSetupService: LedStripSetupService,
) : LedStripSetupApi {

    override fun getStripsForClient(@QueryValue clientUuid: String): HttpResponse<Any> {
        val clientEntityOptional = clientRepository.findByUuid(clientUuid)
        return if (clientEntityOptional.isPresent) {
            val clientEntity = clientEntityOptional.get()
            val stripResponseList = clientEntity.strips.map { s ->
                GetLedStripResponse(
                    clientUuid = clientUuid,
                    name = s.name!!,
                    uuid = s.uuid!!,
                    length = s.length!!,
                    height = s.height,
                    powerLimit = s.powerLimit,
                    blendMode = s.blendMode!!,
                )
            }
            HttpResponse.ok(GetLedStripsResponse(stripResponseList))
        } else {
            HttpResponse.badRequest("No client exists with uuid $clientUuid!")
        }
    }

    override fun getStrip(uuid: String): HttpResponse<Any> {
        val entityOptional = stripRepository.findByUuid(uuid)
        return if (entityOptional.isPresent) {
            val entity = entityOptional.get()
            val response = entity.let { s ->
                GetLedStripResponse(
                    clientUuid = s.client!!.uuid!!,
                    name = s.name!!,
                    uuid = s.uuid!!,
                    length = s.length!!,
                    height = s.height,
                    powerLimit = s.powerLimit,
                    blendMode = s.blendMode!!,
                )
            }
            HttpResponse.ok(response)
        } else {
            HttpResponse.badRequest("No LED strip exists with uuid $uuid!")
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