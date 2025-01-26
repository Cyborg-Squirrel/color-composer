package io.cyborgsquirrel.lighting.effects.repository

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import java.util.*

interface ActiveLightEffectRepository {

    fun addOrUpdateEffect(lightEffect: ActiveLightEffect)

    fun removeEffect(lightEffect: ActiveLightEffect)

    fun findEffectsWithStatus(status: LightEffectStatus): List<ActiveLightEffect>

    fun findEffectWithUuid(uuid: String): Optional<ActiveLightEffect>

    fun findAllEffects(): List<ActiveLightEffect>
}