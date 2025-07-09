package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.lighting.enums.FadeCurve
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class NightriderCometEffectSettings(
    val trailLength: Int,
    val trailFadeCurve: FadeCurve,
    val wrap: Boolean,
    val updatesPerSecond: Int
) : NightriderEffectSettings() {
    override fun wrap() = wrap

    override fun updatesPerSecond() = updatesPerSecond
}