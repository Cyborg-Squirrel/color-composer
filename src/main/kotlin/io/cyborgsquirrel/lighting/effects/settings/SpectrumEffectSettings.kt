package io.cyborgsquirrel.lighting.effects.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class SpectrumEffectSettings(
    val colorPixelWidth: Int,
    val animated: Boolean,
) : LightEffectSettings(1, 0) {
    companion object {
        fun default(numberOfLeds: Int) = SpectrumEffectSettings(numberOfLeds / 10, true)
    }
}