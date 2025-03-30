package io.cyborgsquirrel.setup.controller

import io.cyborgsquirrel.client_config.repository.H2LedStripClientRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripRepository
import io.cyborgsquirrel.entity.LedStripEntity
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.rendering.limits.PowerLimiterService
import io.cyborgsquirrel.setup.api.LedStripSetupApi
import io.cyborgsquirrel.setup.requests.strip.CreateLedStripRequest
import io.cyborgsquirrel.setup.requests.strip.UpdateLedStripRequest
import io.cyborgsquirrel.setup.responses.strip.LedStripResponse
import io.cyborgsquirrel.setup.responses.strip.LedStripsResponse
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.QueryValue
import java.util.*

@Controller("/strip")
class LedStripSetupController(
    private val clientRepository: H2LedStripClientRepository,
    private val stripRepository: H2LedStripRepository,
    private val powerLimiterService: PowerLimiterService
) : LedStripSetupApi {

    override fun getStripsForClient(@QueryValue clientUuid: String): HttpResponse<Any> {
        val clientEntityOptional = clientRepository.findByUuid(clientUuid)
        return if (clientEntityOptional.isPresent) {
            val clientEntity = clientEntityOptional.get()
            val stripResponseList = clientEntity.strips.map { s ->
                LedStripResponse(
                    clientUuid = clientUuid,
                    name = s.name!!,
                    uuid = s.uuid!!,
                    length = s.length!!,
                    height = s.height,
                    powerLimit = s.powerLimit,
                    blendMode = s.blendMode!!,
                )
            }
            HttpResponse.ok(LedStripsResponse(stripResponseList))
        } else {
            HttpResponse.badRequest("No client exists with uuid $clientUuid!")
        }
    }

    override fun getStrip(uuid: String): HttpResponse<Any> {
        val entityOptional = stripRepository.findByUuid(uuid)
        return if (entityOptional.isPresent) {
            val entity = entityOptional.get()
            val response = entity.let { s ->
                LedStripResponse(
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

    override fun createStrip(@QueryValue clientUuid: String, @Body request: CreateLedStripRequest): HttpResponse<Any> {
        val clientEntityOptional = clientRepository.findByUuid(clientUuid)
        if (clientEntityOptional.isPresent) {
            val stripEntity = LedStripEntity(
                client = clientEntityOptional.get(),
                uuid = UUID.randomUUID().toString(),
                name = request.name,
                length = request.length,
                height = request.height ?: 1,
                powerLimit = request.powerLimit,
                blendMode = request.blendMode ?: BlendMode.Additive
            )
            stripRepository.save(stripEntity)
            return HttpResponse.created(stripEntity.uuid)
        } else {
            return HttpResponse.badRequest("No client exists with uuid $clientUuid!")
        }
    }

    override fun updateStrip(uuid: String, @Body updatedStrip: UpdateLedStripRequest): HttpResponse<Any> {
        val entityOptional = stripRepository.findByUuid(uuid)
        return if (entityOptional.isPresent) {
            val entity = entityOptional.get()
            // TODO notify any running effects of the updated length or height. Force user to restart effects?
            // TODO notify renderer or active effect registry of blend mode changes.
            val newEntity = entity.copy(
                name = updatedStrip.name ?: entity.name,
                length = updatedStrip.length ?: entity.length,
                height = updatedStrip.height ?: entity.height,
                powerLimit = updatedStrip.powerLimit ?: entity.powerLimit,
                blendMode = updatedStrip.blendMode ?: entity.blendMode
            )

            if (newEntity != entity) {
                stripRepository.update(newEntity)
                if (updatedStrip.powerLimit != null) powerLimiterService.setLimit(uuid, updatedStrip.powerLimit)
            }

            HttpResponse.noContent()
        } else {
            HttpResponse.badRequest("Client with uuid $uuid does not exist! Please create it first before updating it.")
        }
    }
}