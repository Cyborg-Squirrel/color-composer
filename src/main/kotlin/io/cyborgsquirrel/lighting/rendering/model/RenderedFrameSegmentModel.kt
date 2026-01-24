package io.cyborgsquirrel.lighting.rendering.model

import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.SingleLedStripModel

class RenderedFrameSegmentModel(
    val strip: SingleLedStripModel,
    val sequenceNumber: Short,
    override val frameData: List<RgbColor>,
): FrameModel(frameData)
