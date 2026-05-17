package io.cyborgsquirrel.lighting.effects.settings

sealed class NightriderEffectSettings : LightEffectSettings() {
    abstract val updatesPerSecond: Int
    abstract val wrap: Boolean
}