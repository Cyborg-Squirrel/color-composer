package io.cyborgsquirrel.lighting.effect_palette.settings

import io.cyborgsquirrel.sunrise_sunset.enums.TimeOfDay
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class TimeOfDayPaletteSettings(val paletteMap: Map<TimeOfDay, Palette>)
