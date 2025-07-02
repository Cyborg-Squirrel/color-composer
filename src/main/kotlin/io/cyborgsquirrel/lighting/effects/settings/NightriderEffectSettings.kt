package io.cyborgsquirrel.lighting.effects.settings

// TODO Updates per second settings, update effect logic
sealed class NightriderEffectSettings : LightEffectSettings() {
    abstract fun wrap(): Boolean

    companion object {
        fun default() = NightriderColorFillEffectSettings(false)
    }
}