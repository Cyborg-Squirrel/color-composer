package io.cyborgsquirrel.lighting.rendering.filters

import io.cyborgsquirrel.model.color.RgbColor

/**
 * Reverses a list of [RgbColor]
 */
class ReverseFilter : LightEffectFilter {

    /**
     * Returns a reversed instance of the [rgbList]
     */
    override fun apply(rgbList: List<RgbColor>): List<RgbColor> {
        return rgbList.reversed()
    }
}