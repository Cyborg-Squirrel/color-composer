package io.cyborgsquirrel.lighting.effects.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class NightriderColorFillEffectSettings(
    override val wrap: Boolean = false,
    override val updatesPerSecond: Int = 35,
    val brightnessScaling: Float = 0.2f,
) : NightriderEffectSettings()
