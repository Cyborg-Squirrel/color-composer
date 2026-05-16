package io.cyborgsquirrel.lighting.effects.service

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import reactor.core.publisher.Flux
import java.util.*

interface LightEffectRegistry {

    val updates: Flux<List<ActiveLightEffect>>

    fun addOrUpdateEffect(lightEffect: ActiveLightEffect)

    fun removeEffect(lightEffect: ActiveLightEffect)

    fun getEffectWithUuid(uuid: String): ActiveLightEffect?

    fun getEffectsForClient(clientUuid: String): List<ActiveLightEffect>

    fun getAllEffectsForStrip(stripUuid: String): List<ActiveLightEffect>

    fun getAllEffects(): List<ActiveLightEffect>

    fun removeAllEffects()
}
