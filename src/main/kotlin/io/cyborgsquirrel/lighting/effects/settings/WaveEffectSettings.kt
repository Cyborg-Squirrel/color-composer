package io.cyborgsquirrel.lighting.effects.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class WaveEffectSettings(
    val startPointPercentage: Int = 50,
    val waveLength: Int = 10,
    val repeat: Boolean = false,
    val updatesPerSecond: Int = 30,
) : LightEffectSettings()
