package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.model.color.RgbColor
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class SpectrumLightEffectSettings(
    val colorPixelWidth: Int,
    val colorList: List<RgbColor> = RgbColor.Rainbow,
    val animated: Boolean
) {
    companion object {
        fun default(numberOfLeds: Int) =
            SpectrumLightEffectSettings(numberOfLeds / RgbColor.Rainbow.size, RgbColor.Rainbow, true)
    }
}