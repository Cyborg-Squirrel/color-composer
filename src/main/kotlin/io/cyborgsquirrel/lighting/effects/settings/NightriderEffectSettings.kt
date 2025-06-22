package io.cyborgsquirrel.lighting.effects.settings

sealed interface NightriderEffectSettings {
    fun wrap(): Boolean

    companion object {
        fun default() = NightriderColorFillEffectSettings(false)
    }
}