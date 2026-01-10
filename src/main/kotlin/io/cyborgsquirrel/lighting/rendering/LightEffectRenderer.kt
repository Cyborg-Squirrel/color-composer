package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameSegmentModel

/**
 * Interface for processing and generating frames for active light effects.
 */
interface LightEffectRenderer {

    fun renderFrames(strips: List<LedStripModel>, clientUuid: String): List<RenderedFrameSegmentModel>
}