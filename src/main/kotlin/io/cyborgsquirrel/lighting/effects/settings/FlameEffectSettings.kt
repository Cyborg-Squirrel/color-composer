package io.cyborgsquirrel.lighting.effects.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class FlameEffectSettings(
    val cooling: Int = 11,
    val sparking: Int = 140,
    val sparks: Int = 1,
    val sparkHeight: Int = 3,
    val updatesPerSecond: Int = 30,
) : LightEffectSettings()
