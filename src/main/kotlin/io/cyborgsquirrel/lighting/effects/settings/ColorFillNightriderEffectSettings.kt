package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.model.color.RgbColor
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ColorFillNightriderEffectSettings(
    val colorList: List<RgbColor>
) : NightriderEffectSettings {
    override fun getColors(): List<RgbColor> {
        return colorList
    }
}