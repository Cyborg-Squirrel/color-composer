package io.cyborgsquirrel.lighting.rendering.filters

import io.cyborgsquirrel.lighting.model.RgbColor
import io.micronaut.serde.annotation.Serdeable

/**
 * Reverses a list of [RgbColor]
 */
@Serdeable
class ReverseFilter(uuid: String) : LightEffectFilter(uuid) {

    /**
     * Returns a reversed instance of the [rgbList]
     */
    override fun apply(rgbList: List<RgbColor>): List<RgbColor> {
        return rgbList.reversed()
    }
}