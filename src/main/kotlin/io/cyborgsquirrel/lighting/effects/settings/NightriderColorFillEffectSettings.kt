package io.cyborgsquirrel.lighting.effects.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class NightriderColorFillEffectSettings(
    val wrap: Boolean = false,
    val updatesPerSecond: Int,
    val brightnessScaling: Float,
) : NightriderEffectSettings() {

    override fun wrap() = wrap

    override fun updatesPerSecond() = updatesPerSecond
}