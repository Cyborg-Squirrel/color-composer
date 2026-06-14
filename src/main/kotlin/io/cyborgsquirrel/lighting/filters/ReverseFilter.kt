package io.cyborgsquirrel.lighting.filters

import io.cyborgsquirrel.lighting.model.RgbColor
import io.micronaut.serde.annotation.Serdeable

/**
 * Reverses a buffer of [RgbColor] values
 */
@Serdeable
class ReverseFilter(uuid: String) : LightEffectFilter(uuid) {

    /**
     * Returns a reversed instance of the [buffer]
     */
    override fun apply(buffer: Array<RgbColor>): Array<RgbColor> {
        buffer.reverse()
        return buffer
    }
}