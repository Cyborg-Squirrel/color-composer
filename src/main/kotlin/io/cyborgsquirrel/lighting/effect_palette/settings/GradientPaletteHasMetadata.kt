package io.cyborgsquirrel.lighting.effect_palette.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GradientPaletteHasMetadata(val points: Map<Int, SettingsPalette>) : LightEffectPaletteHasMetadata()
