package io.cyborgsquirrel.lighting.effects.settings

sealed class NightriderEffectSettings : LightEffectSettings() {
    abstract fun wrap(): Boolean

    abstract fun updatesPerSecond(): Int

    companion object {
        fun default() = NightriderColorFillEffectSettings(false, 35)
    }
}