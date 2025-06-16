package io.cyborgsquirrel.lighting.effect_palette.settings

import io.cyborgsquirrel.lighting.model.RgbColor
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class Palette(
    val primaryColor: RgbColor,
    val secondaryColor: RgbColor,
    val tertiaryColor: RgbColor?,
    val otherColors: List<RgbColor>
) {
    fun interpolate(other: Palette, interpolation: Float): Palette {
        val primary = primaryColor.interpolate(other.primaryColor, interpolation)
        val secondary = secondaryColor.interpolate(other.secondaryColor, interpolation)
        val tertiary = if (tertiaryColor != null && other.tertiaryColor != null) tertiaryColor.interpolate(
            other.tertiaryColor,
            interpolation
        ) else null
        val otherCs = otherColors.zip(other.otherColors).map {
            it.first.interpolate(it.second, interpolation)
        }
        return Palette(primary, secondary, tertiary, otherCs)
    }
}
