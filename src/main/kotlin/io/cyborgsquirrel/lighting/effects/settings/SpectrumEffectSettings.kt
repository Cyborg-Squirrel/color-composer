package io.cyborgsquirrel.lighting.effects.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class SpectrumEffectSettings(
    val colorBandPercentage: Int = 10,
    val animated: Boolean = true,
    val updatesPerSecond: Int = 30,
) : LightEffectSettings()
