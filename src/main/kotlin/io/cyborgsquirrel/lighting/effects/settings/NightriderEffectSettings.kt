package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.model.color.RgbColor

sealed interface NightriderEffectSettings {
    fun getColors(): List<RgbColor>

    fun wrap(): Boolean

    companion object {
        fun default() = NightriderColorFillEffectSettings(RgbColor.Rainbow, false)
    }
}