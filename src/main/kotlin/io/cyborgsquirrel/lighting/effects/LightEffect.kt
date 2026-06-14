package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effects.settings.LightEffectSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.util.time.TimeHelper

sealed class LightEffect(
    open val settings: LightEffectSettings,
    open val palette: ColorPalette?,
    protected val timeHelper: TimeHelper,
) {

    abstract fun getNextStep(): Array<RgbColor>

    abstract fun getBuffer(): Array<RgbColor>

    abstract fun getIterations(): Int

    abstract fun updatePalette(palette: ColorPalette)
}