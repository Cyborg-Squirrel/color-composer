package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.micronaut.serde.annotation.Serdeable

// TODO Change starting height to a percentage of the strip
@Serdeable
data class BouncingBallEffectSettings(
    val startingHeight: Double,
    val maxHeight: Int,
    val speed: Double,
    val gravity: Double,
    val minimumSpeed: Double,
) : LightEffectSettings() {
    companion object {
        fun default(numberOfLeds: Int) =
            BouncingBallEffectSettings(
                1.0,
                numberOfLeds - 1,
                4.0,
                LightEffectConstants.EARTH_GRAVITY,
                0.05
            )
    }
}