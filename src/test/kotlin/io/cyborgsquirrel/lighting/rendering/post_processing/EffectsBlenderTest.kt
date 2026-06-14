package io.cyborgsquirrel.lighting.rendering.post_processing

import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.RgbColorPresets
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class EffectsBlenderTest : StringSpec({

    val blender = EffectsBlender()

    "additive blend mode" {
        val strip = mockk<LedStripModel>()
        every { strip.length() } returns 3
        every { strip.blendMode } returns BlendMode.Additive

        val effect1 = arrayOf(
            RgbColor(255u, 0u, 0u),
            RgbColor(0u, 255u, 0u),
            RgbColor(0u, 0u, 255u)
        )

        val effect2 = arrayOf(
            RgbColor(0u, 0u, 0u),
            RgbColor(255u, 0u, 0u),
            RgbColor(0u, 255u, 0u)
        )

        val effect3 = arrayOf(
            RgbColor(0u, 0u, 0u),
            RgbColor(0u, 0u, 0u),
            RgbColor(255u, 255u, 255u)
        )

        val result = blender.blendEffects(strip, listOf(effect1, effect2, effect3))

        result.size shouldBe 3
        result[0] shouldBe RgbColor(255u, 0u, 0u)
        result[1] shouldBe RgbColor(255u, 255u, 0u)
        result[2] shouldBe RgbColor(255u, 255u, 255u) // saturating add clamps each channel at the max value
    }

    "average blend mode" {
        val strip = mockk<LedStripModel>()
        every { strip.length() } returns 3
        every { strip.blendMode } returns BlendMode.Average

        val effect1 = arrayOf(
            RgbColor(255u, 0u, 0u),
            RgbColor(0u, 255u, 0u),
            RgbColor(0u, 0u, 255u)
        )

        val effect2 = arrayOf(
            RgbColor(255u, 0u, 0u),
            RgbColor(0u, 255u, 0u),
            RgbColor(0u, 0u, 255u)
        )

        val effect3 = arrayOf(
            RgbColor(255u, 0u, 0u),
            RgbColor(0u, 255u, 0u),
            RgbColor(0u, 0u, 255u)
        )

        val result = blender.blendEffects(strip, listOf(effect1, effect2, effect3))

        result.size shouldBe 3
        result[0] shouldBe RgbColor(255u, 0u, 0u)
        result[1] shouldBe RgbColor(0u, 255u, 0u)
        result[2] shouldBe RgbColor(0u, 0u, 255u)
    }

    "layer blend mode" {
        val strip = mockk<LedStripModel>()
        every { strip.length() } returns 3
        every { strip.blendMode } returns BlendMode.Layer

        val effect1 = arrayOf(
            RgbColor(255u, 0u, 0u),
            RgbColor(0u, 255u, 0u),
            RgbColor(0u, 0u, 255u)
        )

        val effect2 = arrayOf(
            RgbColor(0u, 0u, 0u),
            RgbColor(255u, 0u, 0u),
            RgbColor(0u, 255u, 0u)
        )

        val effect3 = arrayOf(
            RgbColor(0u, 0u, 0u),
            RgbColor(0u, 0u, 0u),
            RgbColor(255u, 255u, 255u)
        )

        val result = blender.blendEffects(strip, listOf(effect1, effect2, effect3))

        result.size shouldBe 3
        result[0] shouldBe RgbColor(255u, 0u, 0u)
        result[1] shouldBe RgbColor(255u, 255u, 0u)
        result[2] shouldBe RgbColor(255u, 255u, 255u)
    }

    "blend mode with blank colors" {
        val strip = mockk<LedStripModel>()
        every { strip.length() } returns 3
        every { strip.blendMode } returns BlendMode.Additive

        val effect1 = arrayOf(
            RgbColorPresets.blank(),
            RgbColor(0u, 255u, 0u),
            RgbColorPresets.blank()
        )

        val effect2 = arrayOf(
            RgbColor(255u, 0u, 0u),
            RgbColorPresets.blank(),
            RgbColor(0u, 0u, 255u)
        )

        val result = blender.blendEffects(strip, listOf(effect1, effect2))

        result.size shouldBe 3
        result[0] shouldBe RgbColor(255u, 0u, 0u)
        result[1] shouldBe RgbColor(0u, 255u, 0u)
        result[2] shouldBe RgbColor(0u, 0u, 255u)
    }

    "use highest value" {
        val strip = mockk<LedStripModel>()
        every { strip.length() } returns 3
        every { strip.blendMode } returns BlendMode.UseHighest

        val effect1 = arrayOf(
            RgbColorPresets.blank(),
            RgbColor(100u, 255u, 100u),
            RgbColorPresets.blank()
        )

        val effect2 = arrayOf(
            RgbColorPresets.blank(),
            RgbColor(255u, 0u, 0u),
            RgbColor(0u, 0u, 255u)
        )

        val result = blender.blendEffects(strip, listOf(effect1, effect2))

        result.size shouldBe 3
        result[0] shouldBe RgbColorPresets.blank()
        result[1] shouldBe RgbColor(255u, 255u, 100u)
        result[2] shouldBe RgbColor(0u, 0u, 255u)
    }

    "single effect is passed through unchanged" {
        val strip = mockk<LedStripModel>()
        every { strip.length() } returns 3
        every { strip.blendMode } returns BlendMode.Layer

        val effect = arrayOf(
            RgbColor(255u, 0u, 0u),
            RgbColorPresets.blank(),
            RgbColor(0u, 0u, 255u)
        )

        val result = blender.blendEffects(strip, listOf(effect))

        result.size shouldBe 3
        result[0] shouldBe RgbColor(255u, 0u, 0u)
        result[1] shouldBe RgbColorPresets.blank()
        result[2] shouldBe RgbColor(0u, 0u, 255u)
    }
})