package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.rendering.frame.RenderedFrame

interface LightEffectRenderer {
    fun addEffect(lightEffect: ActiveLightEffect)

    fun updateEffect(lightEffect: ActiveLightEffect)

    fun getEffectsWithStatus(status: LightEffectStatus): List<ActiveLightEffect>

    fun renderFrame(lightUuid: String, sequenceNumber: Short): RenderedFrame
}