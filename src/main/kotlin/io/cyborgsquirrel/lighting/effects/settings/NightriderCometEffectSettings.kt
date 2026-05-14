package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.lighting.enums.FadeCurve
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class NightriderCometEffectSettings(
    val trailLength: Int,
    val trailFadeCurve: FadeCurve,
    override val wrap: Boolean,
    override val updatesPerSecond: Int,
) : NightriderEffectSettings()