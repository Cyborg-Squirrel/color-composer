package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class BouncingBallEffectSettings(
    val startingHeightPercent: Int = 1,
    val maxHeightPercent: Int = 100,
    val speed: Double = 4.0,
    val gravity: Double = LightEffectConstants.EARTH_GRAVITY,
    val minimumSpeed: Double = 0.05,
) : LightEffectSettings()
