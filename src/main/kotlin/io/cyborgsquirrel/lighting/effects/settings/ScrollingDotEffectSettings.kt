package io.cyborgsquirrel.lighting.effects.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ScrollingDotEffectSettings(val dotLength: Int, val spaceBetweenDots: Int, val scrollAmountPerSecond: Int) :
    LightEffectSettings() {
    companion object {
        fun default() = ScrollingDotEffectSettings(2, 2, 4)
    }
}
