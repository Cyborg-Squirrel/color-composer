package io.cyborgsquirrel.lighting.effect_palette.settings

import kotlin.time.Duration

sealed class ChangingPaletteSettings {
    abstract fun paletteHoldTime(): Duration
    abstract fun paletteTransitionTime(): Duration
}