package io.cyborgsquirrel.lighting.enums

/**
 * Instructs the [io.cyborgsquirrel.lighting.rendering.LightEffectRenderer] how to handle effects
 * whose rendered RGB output length does not match the length of the LED strip.
 */
enum class EffectLengthMode {
    // Truncate effect output that is longer than the strip down to the strip length. A warning is
    // logged whenever output is actually truncated.
    Truncate,

    // Drop (ignore) any effect whose output length does not exactly equal the strip length. A
    // warning is logged whenever an effect is ignored.
    Ignore,

    // Allow effect output of any length to pass through unchanged.
    Permissive,
}
