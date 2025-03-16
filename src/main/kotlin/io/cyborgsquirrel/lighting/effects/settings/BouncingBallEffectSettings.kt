package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.cyborgsquirrel.model.color.RgbColor

data class BouncingBallEffectSettings(
    val startingHeight: Double,
    val speed: Double,
    val gravity: Double,
    val minimumSpeed: Double,
    val ballColor: RgbColor,
) {
    companion object {
        fun default() = BouncingBallEffectSettings(1.0, 4.0, LightEffectConstants.EARTH_GRAVITY, 0.05, RgbColor.Red)
    }
}