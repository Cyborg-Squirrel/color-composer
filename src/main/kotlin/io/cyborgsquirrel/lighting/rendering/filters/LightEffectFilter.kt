package io.cyborgsquirrel.lighting.rendering.filters

import io.cyborgsquirrel.model.color.RgbColor

sealed class LightEffectFilter(val uuid: String) {
    /**
     * Applies the filter to the [rgbList] and returns a new [RgbColor] list
     */
    abstract fun apply(rgbList: List<RgbColor>): List<RgbColor>
}