package io.cyborgsquirrel.lighting.effect_palette.settings

import io.micronaut.serde.annotation.Serdeable
import java.time.LocalTime

@Serdeable
data class LocalTimePalette(val paletteMap: Map<LocalTime, SettingsPalette>) : LightEffectPalette()
