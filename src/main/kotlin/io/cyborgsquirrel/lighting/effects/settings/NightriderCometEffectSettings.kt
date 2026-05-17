package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.lighting.enums.FadeCurve
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class NightriderCometEffectSettings(
    val trailLength: Int = 5,
    val trailFadeCurve: FadeCurve = FadeCurve.Linear,
    override val wrap: Boolean = false,
    override val updatesPerSecond: Int = 35,
) : NightriderEffectSettings()
