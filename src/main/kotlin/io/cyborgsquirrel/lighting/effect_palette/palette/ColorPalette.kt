package io.cyborgsquirrel.lighting.effect_palette.palette

import io.cyborgsquirrel.lighting.model.RgbColor

sealed class ColorPalette(val uuid: String, val numberOfLeds: Int) {
    /**
     * Returns the primary color for the palette
     */
    abstract fun getPrimaryColor(index: Int): RgbColor

    /**
     * Returns the secondary color for the palette
     */
    abstract fun getSecondaryColor(index: Int): RgbColor

    /**
     * Returns the tertiary color for the palette
     * Tertiary is optional, and may be null.
     */
    abstract fun getTertiaryColor(index: Int): RgbColor?

    /**
     * Returns additional colors for the palette
     * These are optional and the list may be empty.
     */
    abstract fun getOtherColors(index: Int): List<RgbColor>
}