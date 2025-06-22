package io.cyborgsquirrel.lighting.effect_palette.settings

import io.micronaut.serde.annotation.Serdeable
import java.time.Duration

@Serdeable
data class ChangingGradientPaletteSettings(
    val gradientList: List<Map<Int, SettingsPalette>>,
    val holdTime: Duration,
    val transitionTime: Duration
) : ChangingPaletteSettings() {
    override fun paletteHoldTime(): Duration {
        return holdTime
    }

    override fun paletteTransitionTime(): Duration {
        return transitionTime
    }
}
