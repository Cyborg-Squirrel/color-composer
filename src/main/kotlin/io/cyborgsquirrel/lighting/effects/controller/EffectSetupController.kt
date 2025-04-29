package io.cyborgsquirrel.lighting.effects.controller

import io.cyborgsquirrel.lighting.effects.api.EffectSetupApi
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectRequest
import io.cyborgsquirrel.lighting.effects.responses.GetEffectsResponse
import io.cyborgsquirrel.lighting.effects.service.EffectSetupService
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller

@Controller("/effect")
class EffectSetupController(
    private val effectSetupService: EffectSetupService
) : EffectSetupApi {

    override fun getAllEffects(): HttpResponse<GetEffectsResponse> {
        return try {
            val response = effectSetupService.getAllEffects()
            HttpResponse.ok(response)
        } catch (ex: Exception) {
            HttpResponse.serverError()
        }
    }

    override fun getEffectsForStrip(stripUuid: String?, groupUuid: String?): HttpResponse<Any> {
        return try {
            if (!stripUuid.isNullOrBlank()) {
                val response = effectSetupService.getEffectsForStrip(stripUuid)
                HttpResponse.ok(response)
            } else if (!groupUuid.isNullOrBlank()) {
                TODO()
            } else {
                HttpResponse.badRequest("A stripUuid or groupUuid must be specified!")
            }
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError()
        }
    }

    override fun createEffect(
        request: CreateEffectRequest
    ): HttpResponse<Any> {
        return try {
            val uuid = effectSetupService.createEffect(request)
            HttpResponse.created(uuid)
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError()
        }
    }

    override fun updateEffect(uuid: String, request: UpdateEffectRequest): HttpResponse<Any> {
        return try {
            effectSetupService.updateEffect(uuid, request)
            HttpResponse.noContent()
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError()
        }
    }

    override fun deleteEffect(uuid: String): HttpResponse<Any> {
        return try {
            effectSetupService.deleteEffect(uuid)
            HttpResponse.noContent()
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError()
        }
    }
}