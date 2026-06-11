package io.cyborgsquirrel.lighting.effects.controller

import io.cyborgsquirrel.lighting.effects.api.EffectApi
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.CreateEffectSettingsRequest
import io.cyborgsquirrel.lighting.effects.requests.ReassignEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectSettingsRequest
import io.cyborgsquirrel.lighting.effects.requests.UpdateEffectStatusRequest
import io.cyborgsquirrel.lighting.effects.service.EffectApiService
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller

@Controller("/effect")
class EffectController(
    private val effectApiService: EffectApiService
) : EffectApi {

    override fun getEffects(stripUuid: String?, poolUuid: String?): HttpResponse<Any> {
        return if (!stripUuid.isNullOrBlank() && !poolUuid.isNullOrBlank()) {
            throw ClientRequestException("stripUuid and poolUuid provided, request must be one or the other.")
        } else if (!stripUuid.isNullOrBlank()) {
            HttpResponse.ok(effectApiService.getEffectsForStrip(stripUuid))
        } else if (!poolUuid.isNullOrBlank()) {
            HttpResponse.ok(effectApiService.getEffectsForPool(poolUuid))
        } else {
            HttpResponse.ok(effectApiService.getAllEffects())
        }
    }

    override fun getSchemas(): HttpResponse<Any> {
        return HttpResponse.ok(effectApiService.getAllSchemas())
    }

    override fun getEffect(uuid: String): HttpResponse<Any> {
        return HttpResponse.ok(effectApiService.getEffectWithUuid(uuid))
    }

    override fun createEffect(
        request: CreateEffectRequest
    ): HttpResponse<Any> {
        return HttpResponse.created(effectApiService.createEffect(request))
    }

    override fun updateEffect(uuid: String, request: UpdateEffectRequest): HttpResponse<Any> {
        effectApiService.updateEffect(uuid, request)
        return HttpResponse.noContent()
    }

    override fun reassignEffect(
        uuid: String,
        request: ReassignEffectRequest
    ): HttpResponse<Any> {
        effectApiService.reassignEffect(uuid, request)
        return HttpResponse.noContent()
    }

    override fun mediaCommand(request: UpdateEffectStatusRequest): HttpResponse<Any> {
        effectApiService.updateEffectStatus(request)
        return HttpResponse.noContent()
    }

    override fun deleteEffect(uuid: String): HttpResponse<Any> {
        effectApiService.deleteEffect(uuid)
        return HttpResponse.noContent()
    }

    override fun getAllEffectSettings(): HttpResponse<Any> {
        return HttpResponse.ok(effectApiService.getAllEffectSettings())
    }

    override fun getEffectSettings(uuid: String): HttpResponse<Any> {
        return HttpResponse.ok(effectApiService.getEffectSettings(uuid))
    }

    override fun createEffectSettings(request: CreateEffectSettingsRequest): HttpResponse<Any> {
        return HttpResponse.created(effectApiService.createEffectSettings(request))
    }

    override fun updateEffectSettings(uuid: String, request: UpdateEffectSettingsRequest): HttpResponse<Any> {
        effectApiService.updateEffectSettings(uuid, request)
        return HttpResponse.noContent()
    }

    override fun deleteEffectSettings(uuid: String): HttpResponse<Any> {
        effectApiService.deleteEffectSettings(uuid)
        return HttpResponse.noContent()
    }
}
