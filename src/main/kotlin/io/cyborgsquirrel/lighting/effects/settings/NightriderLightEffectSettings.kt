package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.model.color.RgbColor
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class NightriderLightEffectSettings(val colorList: List<RgbColor>) {
    companion object {
        fun default() = NightriderLightEffectSettings(RgbColor.Rainbow)
    }
}