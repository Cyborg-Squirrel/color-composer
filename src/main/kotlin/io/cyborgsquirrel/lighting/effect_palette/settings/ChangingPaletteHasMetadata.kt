package io.cyborgsquirrel.lighting.effect_palette.settings

import java.time.Duration


sealed class ChangingPaletteHasMetadata : LightEffectPaletteHasMetadata() {
    abstract fun paletteHoldTime(): Duration
    abstract fun paletteTransitionTime(): Duration
}