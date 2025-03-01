package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.model.color.RgbColor
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class NightriderColorFillEffectSettings(
    val colorList: List<RgbColor>
) : NightriderEffectSettings {
    override fun getColors(): List<RgbColor> {
        return colorList
    }
}