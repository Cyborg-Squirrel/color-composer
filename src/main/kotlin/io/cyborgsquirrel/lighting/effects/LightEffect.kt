package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effects.settings.LightEffectSettings
import io.cyborgsquirrel.lighting.model.RgbColor

sealed class LightEffect(open val settings: LightEffectSettings, open val palette: ColorPalette?) {
    /**
     * Tells the effect to render the next frame and returns a list of [RgbColor] with a length matching the number of
     * LEDs specified in the effect's constructor
     */
    abstract fun getNextStep(): List<RgbColor>

    /**
     * Returns the buffer rendered by the last [getNextStep] call
     *
     * If [getNextStep] has never been called this returns an empty list
     */
    abstract fun getBuffer(): List<RgbColor>

    /**
     * Returns the number of times the effect has played
     */
    abstract fun getIterations(): Int

    /**
     * Updates the palette in use by the effect
     */
    abstract fun updatePalette(palette: ColorPalette)
}