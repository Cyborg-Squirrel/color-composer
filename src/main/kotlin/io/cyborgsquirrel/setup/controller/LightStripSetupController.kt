package io.cyborgsquirrel.setup.controller

import io.cyborgsquirrel.client_config.repository.H2LedStripClientRepository
import io.cyborgsquirrel.client_config.repository.H2LedStripRepository
import io.cyborgsquirrel.entity.LedStripEntity
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.rendering.limits.PowerLimiterService
import io.cyborgsquirrel.setup.requests.CreateLedStripRequest
import io.cyborgsquirrel.setup.requests.UpdateLedStripRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import java.util.*

@Controller("/strip")
class LightStripSetupController(
    private val clientRepository: H2LedStripClientRepository,
    private val stripRepository: H2LedStripRepository,
    private val powerLimiterService: PowerLimiterService
) {

    @Get
    fun getStripsForClient(@QueryValue clientUuid: String): HttpResponse<Any> {
        val clientEntityOptional = clientRepository.findByUuid(clientUuid)
        return if (clientEntityOptional.isPresent) {
            // TODO map client's strips to serialized LED strips list object
            HttpResponse.serverError("Not implemented")
        } else {
            HttpResponse.badRequest("No client exists with uuid $clientUuid!")
        }
    }

    @Get("/{uuid}")
    fun getStrip(uuid: String): HttpResponse<Any> {
        val entityOptional = stripRepository.findByUuid(uuid)
        return if (entityOptional.isPresent) {
            // TODO map strip to serialized LED strip object
            HttpResponse.serverError("Not implemented")
        } else {
            HttpResponse.badRequest("No LED strip exists with uuid $uuid!")
        }
    }

    @Put
    fun createStrip(
        @QueryValue clientUuid: String, @Body request: CreateLedStripRequest
    ): HttpResponse<Any> {
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

    @Patch("/{uuid}")
    fun updateStrip(uuid: String, @Body request: UpdateLedStripRequest): HttpResponse<Any> {
        val entityOptional = stripRepository.findByUuid(uuid)
        return if (entityOptional.isPresent) {
            val entity = entityOptional.get()
            var newEntity: LedStripEntity? = null

            if (request.name != null) {
                newEntity = entity.copy(name = request.name)
            }

            // TODO notify any running effects of the updated length. Force user to restart effects?
            if (request.length != null) {
                newEntity = entity.copy(length = request.length)
            }

            // TODO notify any running effects of the updated height. Force user to restart effects?
            if (request.height != null) {
                newEntity = entity.copy(height = request.height)
            }

            if (request.powerLimit != null) {
                newEntity = entity.copy(powerLimit = request.powerLimit)
            }

            // TODO notify renderer or active effect registry of blend mode changes.
            if (request.blendMode != null) {
                newEntity = entity.copy(blendMode = request.blendMode)
            }

            if (newEntity != entity) {
                stripRepository.update(newEntity)
                if (request.powerLimit != null) powerLimiterService.setLimit(uuid, request.powerLimit)
            }

            HttpResponse.noContent()
        } else {
            HttpResponse.badRequest("Client with uuid $uuid does not exist! Please create it first before updating it.")
        }
    }
}