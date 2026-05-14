package io.cyborgsquirrel.lighting.effects.settings

sealed class NightriderEffectSettings : LightEffectSettings() {
    abstract val updatesPerSecond: Int
    abstract val wrap: Boolean

    companion object {
        fun default() = NightriderColorFillEffectSettings(false, 35, 0.2f)
    }
}