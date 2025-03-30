package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.lighting.model.RgbColor
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class SpectrumEffectSettings(
    val colorPixelWidth: Int,
    val colorList: List<RgbColor> = RgbColor.Rainbow,
    val animated: Boolean
) {
    companion object {
        fun default(numberOfLeds: Int) =
            SpectrumEffectSettings(numberOfLeds / RgbColor.Rainbow.size, RgbColor.Rainbow, true)
    }
}