package io.cyborgsquirrel.lighting.rendering.model

import io.cyborgsquirrel.lighting.model.RgbColor

data class RenderedFrameModel(
    val timestamp: Long,
    val lightUuid: String,
    val frameData: List<RgbColor>,
    val sequenceNumber: Short,
)
