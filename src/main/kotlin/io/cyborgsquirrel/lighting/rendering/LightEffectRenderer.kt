package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameSegmentModel

/**
 * Interface for processing and generating frames for active light effects.
 */
interface LightEffectRenderer {

    /**
     * Renders all active light effects for the specified LED [strips].
     */
    fun renderFrames(
        strips: List<LedStripModel>,
        clientUuid: String,
    ): List<RenderedFrameSegmentModel>
}