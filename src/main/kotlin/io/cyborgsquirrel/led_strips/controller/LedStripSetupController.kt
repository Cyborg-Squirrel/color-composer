package io.cyborgsquirrel.led_strips.controller

import io.cyborgsquirrel.clients.enums.ClientStatus
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.clients.status.ClientStatusService
import io.cyborgsquirrel.led_strips.api.LedStripSetupApi
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.led_strips.requests.CreateLedStripRequest
import io.cyborgsquirrel.led_strips.requests.UpdateLedStripRequest
import io.cyborgsquirrel.led_strips.responses.GetLedStripResponse
import io.cyborgsquirrel.led_strips.responses.GetLedStripsResponse
import io.cyborgsquirrel.led_strips.service.LedStripApiService
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.registry.ActiveLightEffectRegistry
import io.cyborgsquirrel.lighting.enums.isActive
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.QueryValue
import kotlin.jvm.optionals.getOrNull

@Controller("/strip")
class LedStripSetupController(
    private val clientRepository: H2LedStripClientRepository,
    private val stripRepository: H2LedStripRepository,
    private val stripSetupService: LedStripApiService,
    private val activeLightEffectRegistry: ActiveLightEffectRegistry,
    private val clientStatusService: ClientStatusService,
) : LedStripSetupApi {

    override fun getStrips(@QueryValue clientUuid: String?): HttpResponse<Any> {
        if (clientUuid != null) {
            val clientEntityOptional = clientRepository.findByUuid(clientUuid)
            return if (clientEntityOptional.isPresent) {
                val activeEffects = activeLightEffectRegistry.getAllEffects().filter { it.status.isActive() }
                val clientEntity = clientEntityOptional.get()
                val clientStatusOptional = clientStatusService.getStatusForClient(clientEntity)
                val stripResponseList = clientEntity.strips.map { s ->
                    GetLedStripResponse(
                        clientUuid = clientUuid,
                        name = s.name!!,
                        uuid = s.uuid!!,
                        pin = s.pin!!,
                        length = s.length!!,
                        height = s.height,
                        powerLimit = s.powerLimit,
                        brightness = s.brightness!!,
                        blendMode = s.blendMode!!,
                        activeEffects = getActiveEffectsForStrip(clientStatusOptional.getOrNull()?.status, activeEffects, s.uuid)
                    )
                }
                HttpResponse.ok(GetLedStripsResponse(stripResponseList))
            } else {
                HttpResponse.badRequest("No client exists with uuid $clientUuid!")
            }
        } else {
            val clientEntities = clientRepository.queryAll()
            val stripEntities = clientEntities.map { it.strips }.flatten()
            val stripResponseList = stripEntities.map { s ->
                GetLedStripResponse(
                    clientUuid = s.client!!.uuid!!,
                    name = s.name!!,
                    uuid = s.uuid!!,
                    pin = s.pin!!,
                    length = s.length!!,
                    height = s.height,
                    powerLimit = s.powerLimit,
                    brightness = s.brightness!!,
                    blendMode = s.blendMode!!,
                    activeEffects = getActiveEffectsForStrip(clientStatusService.getStatusForClient(s.client!!).getOrNull()?.status,
                        activeLightEffectRegistry.getAllEffects().filter { it.status.isActive() }, s.uuid),
                )
            }

            return HttpResponse.ok(GetLedStripsResponse(stripResponseList))
        }
    }

    override fun getStrip(uuid: String): HttpResponse<Any> {
        val entityOptional = stripRepository.findByUuid(uuid)
        return if (entityOptional.isPresent) {
            val activeEffects = activeLightEffectRegistry.getAllEffects().filter { it.status.isActive() }
            val entity = entityOptional.get()
            val response = entity.let { s ->
                GetLedStripResponse(
                    clientUuid = s.client!!.uuid!!,
                    name = s.name!!,
                    uuid = s.uuid!!,
                    pin = s.pin!!,
                    length = s.length!!,
                    height = s.height,
                    powerLimit = s.powerLimit,
                    brightness = s.brightness!!,
                    blendMode = s.blendMode!!,
                    activeEffects = activeEffects.filter { it.strip.getUuid() == s.uuid }.size,
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

    // The effect could be configured as Playing but the client is offline. If the client is offline
    // we should report 0 active effects.
    private fun getActiveEffectsForStrip(
        clientStatus: ClientStatus?,
        activeEffects: List<ActiveLightEffect>,
        stripUuid: String?
    ): Int {
        return if (clientStatus == ClientStatus.Active) activeEffects.filter { it.strip.getUuid() == stripUuid }.size else 0
    }
}