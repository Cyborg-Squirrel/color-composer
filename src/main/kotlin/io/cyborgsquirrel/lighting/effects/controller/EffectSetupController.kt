package io.cyborgsquirrel.lighting.effects.controller

import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.lighting.effects.api.EffectSetupApi
import io.cyborgsquirrel.lighting.effects.repository.H2LightEffectRepository
import io.cyborgsquirrel.lighting.effects.responses.GetEffectResponse
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller

@Controller("/effect")
class EffectSetupController(
    private val stripRepository: H2LedStripRepository,
    private val effectRepository: H2LightEffectRepository
) : EffectSetupApi {

    override fun getAllEffects(): HttpResponse<Any> {
        // TODO strip vs strip group differentiation, strip group support
        val effectEntities = effectRepository.queryAll()
        val effectList = effectEntities.map {
            GetEffectResponse(
                name = it.name!!,
                uuid = it.uuid!!,
                stripUuid = it.strip!!.uuid!!,
                settings = it.settings!!,
                status = it.status!!,
            )
        }
        return HttpResponse.ok(effectList)
    }

    // TODO strip group support
    override fun getEffectsForStrip(stripUuid: String): HttpResponse<Any> {
        val stripEntityOptional = stripRepository.findByUuid(stripUuid)
        return if (stripEntityOptional.isPresent) {
            val strip = stripEntityOptional.get()
            val effectList = strip.effects.map {
                GetEffectResponse(
                    name = it.name!!,
                    uuid = it.uuid!!,
                    stripUuid = strip.uuid!!,
                    settings = it.settings!!,
                    status = it.status!!,
                )
            }
            HttpResponse.ok(effectList)
        } else {
            HttpResponse.badRequest("Could not get effect. Strip with uuid $stripUuid does not exist!")
        }
    }

    override fun createEffect(stripUuid: String): HttpResponse<Any> {
        TODO("Not yet implemented")
    }

    override fun updateEffect(uuid: String): HttpResponse<Any> {
        TODO("Not yet implemented")
    }

    override fun deleteEffect(uuid: String): HttpResponse<Any> {
        TODO("Not yet implemented")
    }
}