package io.cyborgsquirrel.lighting.enums

/**
 * Enum instructing the [io.cyborgsquirrel.lighting.rendering.LightEffectRenderer]
 * how to render multiple effects on the same LED strip
 */
enum class BlendMode {
    // Adds the RGB values together (examples: red + blue = purple, red + blue + green = white)
    Additive,

    // Average the RGB values when effects light the same LED
    Average,

    // Only display the RGB values from the effect with higher priority
    Layer,

    // Use the highest red, green, and blue values
    // For example a RGB value of 100, 50, 20 and another 50, 75, 0 will result in 100, 75, 20
    UseHighest,
}