package io.cyborgsquirrel.lighting.enums

import io.cyborgsquirrel.lighting.effects.LightEffectConstants
import io.micronaut.serde.annotation.Serdeable

@Serdeable
enum class EffectCategory {
    Static,
    Ambient,
    Motion;

    companion object {
        fun forEffect(type: String): EffectCategory {
            return when (type) {
                LightEffectConstants.SPECTRUM_NAME -> Static
                LightEffectConstants.FLAME_EFFECT_NAME,
                LightEffectConstants.WAVE_EFFECT_NAME,
                LightEffectConstants.MARQUEE_EFFECT_NAME -> Ambient
                LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME,
                LightEffectConstants.NIGHTRIDER_COMET_NAME,
                LightEffectConstants.BOUNCING_BALL_NAME -> Motion
                else -> Static
            }
        }
    }
}
