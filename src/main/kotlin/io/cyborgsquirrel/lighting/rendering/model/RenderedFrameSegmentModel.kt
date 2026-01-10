package io.cyborgsquirrel.lighting.rendering.model

import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.SingleLedStripModel
import io.cyborgsquirrel.lighting.rendering.cache.MIN_SEQUENCE_NUMBER

class RenderedFrameSegmentModel(
    val strip: SingleLedStripModel,
    val sequenceNumber: Short,
    override val frameData: List<RgbColor>,
): FrameModel(frameData)
