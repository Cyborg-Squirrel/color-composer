package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.model.color.RgbColor

sealed interface NightriderEffectSettings {
    fun getColors(): List<RgbColor>

    companion object {
        fun default() = ColorFillNightriderEffectSettings(RgbColor.Rainbow)
    }
}