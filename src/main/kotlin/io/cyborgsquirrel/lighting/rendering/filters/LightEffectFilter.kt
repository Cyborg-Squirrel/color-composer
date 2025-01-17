package io.cyborgsquirrel.lighting.rendering.filters

import io.cyborgsquirrel.model.color.RgbColor

interface LightEffectFilter {
    /**
     * Applies the filter to the [rgbList] and returns a new [RgbColor] list
     */
    fun apply(rgbList: List<RgbColor>): List<RgbColor>
}