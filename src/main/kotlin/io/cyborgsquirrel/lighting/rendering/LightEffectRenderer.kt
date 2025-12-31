package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameModel
import java.util.*

/**
 * Interface for processing and generating frames for active light effects.
 */
interface LightEffectRenderer {

    fun renderFrames(stripUuids: List<String>, sequenceNumber: Short): List<RenderedFrameModel>

    fun renderFrame(stripUuid: String, sequenceNumber: Short): Optional<RenderedFrameModel>
}