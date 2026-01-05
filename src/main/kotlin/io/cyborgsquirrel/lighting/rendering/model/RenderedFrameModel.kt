package io.cyborgsquirrel.lighting.rendering.model

import io.cyborgsquirrel.lighting.model.RgbColor

data class RenderedFrameModel(
    val stripUuid: String,
    val frameData: List<RgbColor>,
    val sequenceNumber: Short,
    val allEffectsPaused: Boolean
)
