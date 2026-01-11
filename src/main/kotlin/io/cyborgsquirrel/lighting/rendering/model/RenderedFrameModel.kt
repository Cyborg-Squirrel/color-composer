package io.cyborgsquirrel.lighting.rendering.model

import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.rendering.cache.MIN_SEQUENCE_NUMBER

class RenderedFrameModel(
    val strip: LedStripModel,
    override val frameData: List<RgbColor>
): FrameModel(frameData) {

    constructor(strip: LedStripModel, frameData: List<RgbColor>, sequenceNumber: Short) : this(strip, frameData) {
        this.sequenceNumber = sequenceNumber
    }

    var sequenceNumber: Short = MIN_SEQUENCE_NUMBER
}
