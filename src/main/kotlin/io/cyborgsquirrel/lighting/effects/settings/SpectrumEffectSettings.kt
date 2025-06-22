package io.cyborgsquirrel.lighting.effects.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class SpectrumEffectSettings(
    val colorPixelWidth: Int,
    val animated: Boolean
) : LightEffectSettings() {
    companion object {
        fun default(numberOfLeds: Int) = SpectrumEffectSettings(numberOfLeds / 10, true)
    }
}