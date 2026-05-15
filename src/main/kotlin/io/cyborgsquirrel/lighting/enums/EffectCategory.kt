package io.cyborgsquirrel.lighting.enums

import io.cyborgsquirrel.lighting.effects.LightEffectType
import io.micronaut.serde.annotation.Serdeable

@Serdeable
enum class EffectCategory {
    Static,
    Ambient,
    Motion;

    companion object {
        fun forEffect(type: String): EffectCategory {
            val effectType = LightEffectType.fromNameOrNull(type) ?: return Static
            return when (effectType) {
                LightEffectType.SPECTRUM, LightEffectType.SPARKLE -> Static
                LightEffectType.FLAME, LightEffectType.WAVE, LightEffectType.MARQUEE -> Ambient
                LightEffectType.NIGHTRIDER_COLOR_FILL, LightEffectType.NIGHTRIDER_COMET, LightEffectType.BOUNCING_BALL -> Motion
            }
        }
    }
}
