package io.cyborgsquirrel.engine.rendering

import io.cyborgsquirrel.engine.effects.ActiveLightEffect
import io.cyborgsquirrel.engine.enums.BlendMode
import io.cyborgsquirrel.engine.rendering.frame.RenderedFrame

interface LedStripRenderer {
    fun addEffect(lightEffect: ActiveLightEffect)

    fun getActiveEffects(): List<ActiveLightEffect>

    fun renderFrame(lightUuid: String, sequenceNumber: Short): RenderedFrame

    fun getRenderFps(effectInstanceUuid: String)

    fun setRenderFps(effectInstanceUuid: String, fps: Int)

    fun setBlendMode(lightUuid: String, blendMode: BlendMode)

    fun pauseEffect(lightUuid: String, effectUuid: String)
}