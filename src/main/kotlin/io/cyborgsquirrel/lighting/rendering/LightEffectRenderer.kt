package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.rendering.frame.RenderedFrame

interface LightEffectRenderer {

    fun renderFrame(lightUuid: String, sequenceNumber: Short): RenderedFrame
}