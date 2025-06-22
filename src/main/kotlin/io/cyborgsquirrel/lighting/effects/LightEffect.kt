package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.model.RgbColor

interface LightEffect {
    /**
     * Tells the effect to render the next frame and returns a list of [RgbColor] with a length matching the number of
     * LEDs specified in the effect's constructor
     */
    fun getNextStep(): List<RgbColor>

    /**
     * Returns the current settings for the LightEffect
     */
    fun getSettings(): Any

    /**
     * Returns the number of times the effect has played
     */
    fun getIterations(): Int

    /**
     * Updates the palette in use by the effect
     */
    fun updatePalette(palette: ColorPalette)
}