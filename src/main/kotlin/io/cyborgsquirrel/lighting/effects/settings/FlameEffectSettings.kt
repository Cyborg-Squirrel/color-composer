package io.cyborgsquirrel.lighting.effects.settings

data class FlameEffectSettings(
    val cooling: Int,
    val sparking: Int,
    val sparks: Int,
    val sparkHeight: Int
) {
    companion object {
        fun default() = FlameEffectSettings(11, 140, 1, 3)
    }
}