package io.cyborgsquirrel.lighting.filters

import io.cyborgsquirrel.lighting.model.RgbColor

sealed class LightEffectFilter(val uuid: String) {
    /**
     * Applies the filter to the [buffer] and returns a new [RgbColor] array
     */
    abstract fun apply(buffer: Array<RgbColor>): Array<RgbColor>
}
