package io.cyborgsquirrel.engine.enums

/**
 * Enum instructing the [io.cyborgsquirrel.engine.rendering.LedStripRenderer]
 * how to render multiple effects on the same LED strip
 */
enum class BlendMode {
    // Average the RGB values when effects light the same LED
    Average,

    // Only the RGB values from the effect with higher priority is shown when
    // effects light the same LED
    Priority,

    // Similar to average but takes priority into effect which means RGB
    // values for higher priority effects are weighted higher in the average
    Layer,
}