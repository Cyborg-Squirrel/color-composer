package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.enums.EffectLengthMode
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameSegmentModel

/**
 * Interface for processing and generating frames for active light effects.
 */
interface LightEffectRenderer {

    /**
     * Renders all active light effects for the specified LED [strips].
     *
     * [lengthMode] controls how effects whose output length does not match the strip length are
     * handled (truncated, ignored, or allowed through).
     */
    fun renderFrames(
        strips: List<LedStripModel>,
        clientUuid: String,
        lengthMode: EffectLengthMode = EffectLengthMode.Permissive,
    ): List<RenderedFrameSegmentModel>
}