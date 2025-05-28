package io.cyborgsquirrel.lighting.effects.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
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