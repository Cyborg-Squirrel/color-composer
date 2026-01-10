package io.cyborgsquirrel.lighting.rendering.model

import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.model.RgbColor

data class RenderedFrameModel(
    val strip: LedStripModel,
    val frameData: List<RgbColor>,
    val sequenceNumber: Short,
    val allEffectsPaused: Boolean
)
