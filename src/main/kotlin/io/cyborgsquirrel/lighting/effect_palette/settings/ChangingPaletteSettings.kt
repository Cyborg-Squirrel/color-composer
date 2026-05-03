package io.cyborgsquirrel.lighting.effect_palette.settings

import java.time.Duration


sealed class ChangingPaletteSettings : LightEffectPaletteSettings(1, 0) {
    abstract fun paletteHoldTime(): Duration
    abstract fun paletteTransitionTime(): Duration
}