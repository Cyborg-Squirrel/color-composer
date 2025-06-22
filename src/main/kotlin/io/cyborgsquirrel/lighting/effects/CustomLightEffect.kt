package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effects.settings.LightEffectSettings
import io.cyborgsquirrel.lighting.model.RgbColor

/**
 * Class for user generated custom lighting effects
 */
class CustomLightEffect : LightEffect {

    override fun getNextStep(): List<RgbColor> {
        TODO("Not yet implemented")
    }

    override fun getSettings(): LightEffectSettings {
        TODO("Not yet implemented")
    }

    override fun getIterations(): Int {
        TODO("Not yet implemented")
    }

    override fun updatePalette(palette: ColorPalette) {
        TODO("Not yet implemented")
    }
}