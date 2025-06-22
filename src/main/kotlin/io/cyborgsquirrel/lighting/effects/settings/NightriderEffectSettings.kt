package io.cyborgsquirrel.lighting.effects.settings

sealed class NightriderEffectSettings : LightEffectSettings() {
    abstract fun wrap(): Boolean

    companion object {
        fun default() = NightriderColorFillEffectSettings(false)
    }
}