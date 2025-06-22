package io.cyborgsquirrel.lighting.effect_palette.settings

import io.micronaut.serde.annotation.Serdeable
import java.time.Duration

@Serdeable
data class ChangingStaticPaletteSettings(
    val palettes: List<SettingsPalette>,
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
