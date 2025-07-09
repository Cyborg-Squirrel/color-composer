package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class BouncingBallEffectSettings(
    val startingHeightPercent: Int,
    val maxHeightPercent: Int,
    val speed: Double,
    val gravity: Double,
    val minimumSpeed: Double,
) : LightEffectSettings() {
    companion object {
        fun default(numberOfLeds: Int) =
            BouncingBallEffectSettings(
                1,
                numberOfLeds - 1,
                4.0,
                LightEffectConstants.EARTH_GRAVITY,
                0.05
            )
    }
}