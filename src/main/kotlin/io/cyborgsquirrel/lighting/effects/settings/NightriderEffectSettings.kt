package io.cyborgsquirrel.lighting.effects.settings

sealed class NightriderEffectSettings : LightEffectSettings(1, 0) {
    abstract fun wrap(): Boolean

    abstract fun updatesPerSecond(): Int

    companion object {
        fun default() = NightriderColorFillEffectSettings(false, 35, 0.2f)
    }
}