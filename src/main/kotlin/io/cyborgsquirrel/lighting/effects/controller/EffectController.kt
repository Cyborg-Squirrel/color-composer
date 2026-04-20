package io.cyborgsquirrel.lighting.effects.controller

import io.cyborgsquirrel.lighting.effects.api.EffectApi
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectStatusRequest
import io.cyborgsquirrel.lighting.effects.responses.GetEffectsResponse
import io.cyborgsquirrel.lighting.effects.service.EffectApiService
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.cyborgsquirrel.util.exception.ResourceNotFoundException
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller

@Controller("/effect")
class EffectController(
    private val effectApiService: EffectApiService
) : EffectApi {

    override fun getEffects(stripUuid: String?, poolUuid: String?): HttpResponse<Any> {
        return try {
            if (!stripUuid.isNullOrBlank() && !poolUuid.isNullOrBlank()) {
                HttpResponse.badRequest("stripUuid and poolUuid provided, request must be one or the other.")
            } else if (!stripUuid.isNullOrBlank()) {
                val response = effectApiService.getEffectsForStrip(stripUuid)
                HttpResponse.ok(response)
            } else if (!poolUuid.isNullOrBlank()) {
                val response = effectApiService.getEffectsForPool(poolUuid)
                HttpResponse.ok(response)
            } else {
                val response = effectApiService.getAllEffects()
                HttpResponse.ok(response)
            }
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun getEffect(uuid: String): HttpResponse<Any> {
        return try {
            val response = effectApiService.getEffectWithUuid(uuid)
            HttpResponse.ok(response)
        } catch (rnfe: ResourceNotFoundException) {
            HttpResponse.notFound()
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun createEffect(
        request: CreateEffectRequest
    ): HttpResponse<Any> {
        return try {
            val uuid = effectApiService.createEffect(request)
            HttpResponse.created(uuid)
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun updateEffect(uuid: String, request: UpdateEffectRequest): HttpResponse<Any> {
        return try {
            effectApiService.updateEffect(uuid, request)
            HttpResponse.noContent()
        } catch (rnfe: ResourceNotFoundException) {
            HttpResponse.notFound()
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun updateEffectStatuses(request: UpdateEffectStatusRequest): HttpResponse<Any> {
        return try {
            effectApiService.updateEffectStatus(request)
            HttpResponse.noContent()
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun deleteEffect(uuid: String): HttpResponse<Any> {
        return try {
            effectApiService.deleteEffect(uuid)
            HttpResponse.noContent()
        } catch (rnfe: ResourceNotFoundException) {
            HttpResponse.notFound()
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }
}