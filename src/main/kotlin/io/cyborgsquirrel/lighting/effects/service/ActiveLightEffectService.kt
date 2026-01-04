package io.cyborgsquirrel.lighting.effects.service

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import java.util.*

interface ActiveLightEffectService {

    fun addOrUpdateEffect(lightEffect: ActiveLightEffect)

    fun removeEffect(lightEffect: ActiveLightEffect)

    fun getEffectWithUuid(uuid: String): Optional<ActiveLightEffect>

    fun getEffectsForClient(clientUuid: String): List<ActiveLightEffect>

    fun getAllEffectsForStrip(stripUuid: String): List<ActiveLightEffect>

    fun getAllEffects(): List<ActiveLightEffect>

    fun addListener(listener: ActiveLightEffectChangeListener)

    fun removeLister(listener: ActiveLightEffectChangeListener)

    fun removeAllEffects()
}