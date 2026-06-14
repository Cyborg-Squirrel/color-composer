package io.cyborgsquirrel.lighting.effect_palette.settings

import io.cyborgsquirrel.lighting.model.RgbColor
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class SettingsPalette(
    val primaryColor: RgbColor,
    val secondaryColor: RgbColor,
    val tertiaryColor: RgbColor?,
    val otherColors: List<RgbColor>
) {
    fun interpolate(other: SettingsPalette, interpolation: Float): SettingsPalette {
        val primary = primaryColor.copy().interpolate(other.primaryColor, interpolation)
        val secondary = secondaryColor.copy().interpolate(other.secondaryColor, interpolation)
        val tertiary = if (tertiaryColor != null && other.tertiaryColor != null) tertiaryColor.copy().interpolate(
            other.tertiaryColor,
            interpolation
        ) else null
        val otherCs = otherColors.zip(other.otherColors).map {
            it.first.copy().interpolate(it.second, interpolation)
        }
        return SettingsPalette(primary, secondary, tertiary, otherCs)
    }
}
