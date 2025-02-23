package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameModel
import java.util.*

/**
 * Interface for processing and generating frames based on active light effects.
 *
 * This interface is utilized to sequentially render frames corresponding to light effects associated
 * with a specific led strip identifier (UUID).
 */
interface LightEffectRenderer {

    fun renderFrame(lightUuid: String, sequenceNumber: Short): Optional<RenderedFrameModel>
}