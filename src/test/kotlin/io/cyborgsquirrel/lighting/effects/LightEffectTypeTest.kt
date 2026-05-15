package io.cyborgsquirrel.lighting.effects

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LightEffectTypeTest : StringSpec({

    "fromName returns correct type for valid display name" {
        LightEffectType.fromName("Spectrum") shouldBe LightEffectType.SPECTRUM
        LightEffectType.fromName("Comet") shouldBe LightEffectType.NIGHTRIDER_COMET
        LightEffectType.fromName("Sparkle") shouldBe LightEffectType.SPARKLE
    }

    "fromName throws IllegalArgumentException for unknown name" {
        shouldThrow<IllegalArgumentException> { LightEffectType.fromName("foo") }
        shouldThrow<IllegalArgumentException> { LightEffectType.fromName("") }
    }

    "fromNameOrNull returns correct type for valid display name" {
        LightEffectType.fromNameOrNull("Flame") shouldBe LightEffectType.FLAME
        LightEffectType.fromNameOrNull("Bouncing Ball") shouldBe LightEffectType.BOUNCING_BALL
    }

    "fromNameOrNull returns null for unknown name" {
        LightEffectType.fromNameOrNull("bar") shouldBe null
        LightEffectType.fromNameOrNull("") shouldBe null
    }
})
