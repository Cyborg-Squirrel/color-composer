package io.cyborgsquirrel.lighting.enums

/**
 * Enum instructing the [io.cyborgsquirrel.lighting.rendering.LightEffectRenderer]
 * how to render multiple effects on the same LED strip
 */
enum class BlendMode {
    // Average the RGB values when effects light the same LED
    Average,

    // Only display the RGB values from the effect with higher priority
    Layer,
}