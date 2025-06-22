package io.cyborgsquirrel.lighting.effects.settings

import io.micronaut.serde.annotation.Serdeable
import kotlin.math.min

@Serdeable
data class WaveEffectSettings(
    val startPoint: Int, val waveLength: Int, val repeat: Boolean,
) : LightEffectSettings() {
    companion object {
        fun default(numberOfLeds: Int) =
            WaveEffectSettings(
                numberOfLeds / 2, min(numberOfLeds / 6, 10), false
            )
    }
}