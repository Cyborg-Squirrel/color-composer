package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameModel
import java.util.*

/**
 * Interface for processing and generating frames for active light effects.
 */
interface LightEffectRenderer {

    fun renderFrame(strip: LedStripModel, sequenceNumber: Short): RenderedFrameModel?
}