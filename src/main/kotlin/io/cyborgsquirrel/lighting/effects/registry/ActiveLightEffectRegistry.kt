package io.cyborgsquirrel.lighting.effects.registry

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import java.util.*

interface ActiveLightEffectRegistry {

    fun addOrUpdateEffect(lightEffect: ActiveLightEffect)

    fun removeEffect(lightEffect: ActiveLightEffect)

    fun findEffectsWithStatus(status: LightEffectStatus): List<ActiveLightEffect>

    fun getEffectWithUuid(uuid: String): Optional<ActiveLightEffect>

    fun getAllEffectsForStrip(stripUuid: String): List<ActiveLightEffect>

    fun getAllEffects(): List<ActiveLightEffect>

    fun reset()
}