package io.cyborgsquirrel.lighting.effects.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class MarqueeEffectSettings(val dotLength: Int, val spaceBetweenDots: Int, val scrollAmountPerSecond: Int) :
    LightEffectSettings() {
    companion object {
        fun default() = MarqueeEffectSettings(2, 2, 4)
    }
}
