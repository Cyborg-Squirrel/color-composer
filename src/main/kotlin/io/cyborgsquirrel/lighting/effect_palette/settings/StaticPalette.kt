package io.cyborgsquirrel.lighting.effect_palette.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class StaticPalette(val palette: SettingsPalette) : LightEffectPalette()
