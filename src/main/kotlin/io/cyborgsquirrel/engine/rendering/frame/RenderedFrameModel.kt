package io.cyborgsquirrel.engine.rendering.frame

import io.cyborgsquirrel.model.color.RgbColor

class RenderedFrameModel(
    timestamp: Long,
    lightUuid: String,
    frameData: List<RgbColor>,
    sequenceNumber: Short,
) : RenderedFrame(timestamp, lightUuid, frameData, sequenceNumber)
