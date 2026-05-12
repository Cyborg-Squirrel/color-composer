package io.cyborgsquirrel.lighting.effect_palette.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GradientPalette(val points: Map<Int, SettingsPalette>) : LightEffectPalette()
