package io.cyborgsquirrel.lighting.effects.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class MarqueeEffectSettings(
    val dotLength: Int = 2,
    val spaceBetweenDots: Int = 2,
    val updatesPerSecond: Int = 8,
) : LightEffectSettings()
