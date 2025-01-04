package io.cyborgsquirrel.engine.rendering.frame

import io.cyborgsquirrel.model.color.RgbColor

abstract class RenderedFrame(
    val timestamp: Long,
    val lightUuid: String,
    val frameData: List<RgbColor>,
    val sequenceNumber: Short,
)