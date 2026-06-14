package io.cyborgsquirrel.lighting.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class RgbColorTest: StringSpec({

    "Add" {
        val purple = RgbColorPresets.red() + RgbColorPresets.blue()
        purple.red shouldBe 255u
        purple.green shouldBe 0u
        purple.blue shouldBe 255u

        val redCyanMix = RgbColor(2u, 3u, 4u) + RgbColor(4u, 4u, 4u)
        redCyanMix.red shouldBe 6u
        redCyanMix.green shouldBe 7u
        redCyanMix.blue shouldBe 8u
    }

    "Interpolate" {
        val redGreenMix = RgbColorPresets.red().interpolate(RgbColorPresets.green(), 0.5f)
        redGreenMix.red shouldBe 127u
        redGreenMix.green shouldBe 127u
        redGreenMix.blue shouldBe 0u

        val orangeBlueMix = RgbColorPresets.orange().interpolate(RgbColorPresets.blue(), 0.5f)
        orangeBlueMix.red shouldBe 127u
        orangeBlueMix.green shouldBe 25u
        orangeBlueMix.blue shouldBe 127u
    }
})