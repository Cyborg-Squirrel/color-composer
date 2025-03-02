package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.lighting.enums.FadeCurve
import io.cyborgsquirrel.model.color.RgbColor
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class NightriderCometEffectSettings(
    val colorList: List<RgbColor>,
    val trailLength: Int,
    val trailFadeCurve: FadeCurve,
) : NightriderEffectSettings {
    override fun getColors(): List<RgbColor> {
        return colorList
    }
}