package io.cyborgsquirrel.lighting.effects

enum class LightEffectType(val displayName: String) {
    SPECTRUM("Spectrum"),
    NIGHTRIDER_COLOR_FILL("Nightrider Color Fill"),
    NIGHTRIDER_COMET("Comet"),
    FLAME("Flame"),
    BOUNCING_BALL("Bouncing Ball"),
    WAVE("Wave"),
    MARQUEE("Marquee"),
    SPARKLE("Sparkle");

    companion object {
        fun fromName(name: String): LightEffectType =
            entries.firstOrNull { it.displayName == name }
                ?: throw IllegalArgumentException("Unknown LightEffect: $name")

        fun fromNameOrNull(name: String): LightEffectType? =
            entries.firstOrNull { it.displayName == name }
    }
}
