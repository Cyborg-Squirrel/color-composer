package io.cyborgsquirrel.lighting.rendering.post_processing

import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.RgbColorPresets
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class EffectsBlenderTest : StringSpec({

    val blender = EffectsBlender()

    // Drives the incremental fold API the renderer uses: reset a reusable accumulator, then fold each effect in order.
    fun blend(blendMode: BlendMode, length: Int, effects: List<Array<RgbColor>>): Array<RgbColor> {
        val accumulator = Array(length) { RgbColorPresets.blank() }
        blender.reset(accumulator)
        effects.forEachIndexed { index, effect -> blender.fold(blendMode, accumulator, effect, index) }
        return accumulator
    }

    "additive blend mode" {
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

        val result = blend(BlendMode.Additive, 3, listOf(effect1, effect2, effect3))

        result.size shouldBe 3
        result[0] shouldBe RgbColor(255u, 0u, 0u)
        result[1] shouldBe RgbColor(255u, 255u, 0u)
        result[2] shouldBe RgbColor(255u, 255u, 255u) // saturating add clamps each channel at the max value
    }

    "average blend mode" {
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

        val result = blend(BlendMode.Average, 3, listOf(effect1, effect2, effect3))

        result.size shouldBe 3
        result[0] shouldBe RgbColor(255u, 0u, 0u)
        result[1] shouldBe RgbColor(0u, 255u, 0u)
        result[2] shouldBe RgbColor(0u, 0u, 255u)
    }

    "average blend mode computes the running-count mean" {
        val effect1 = arrayOf(RgbColor(200u, 0u, 90u))
        val effect2 = arrayOf(RgbColor(100u, 0u, 30u))

        val result = blend(BlendMode.Average, 1, listOf(effect1, effect2))

        // (200 + 100) / 2 = 150, (90 + 30) / 2 = 60
        result[0] shouldBe RgbColor(150u, 0u, 60u)
    }

    "layer blend mode" {
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

        val result = blend(BlendMode.Layer, 3, listOf(effect1, effect2, effect3))

        result.size shouldBe 3
        result[0] shouldBe RgbColor(255u, 0u, 0u)
        result[1] shouldBe RgbColor(255u, 255u, 0u)
        result[2] shouldBe RgbColor(255u, 255u, 255u)
    }

    "blend mode with blank colors" {
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

        val result = blend(BlendMode.Additive, 3, listOf(effect1, effect2))

        result.size shouldBe 3
        result[0] shouldBe RgbColor(255u, 0u, 0u)
        result[1] shouldBe RgbColor(0u, 255u, 0u)
        result[2] shouldBe RgbColor(0u, 0u, 255u)
    }

    "use highest value" {
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

        val result = blend(BlendMode.UseHighest, 3, listOf(effect1, effect2))

        result.size shouldBe 3
        result[0] shouldBe RgbColorPresets.blank()
        result[1] shouldBe RgbColor(255u, 255u, 100u)
        result[2] shouldBe RgbColor(0u, 0u, 255u)
    }

    "single effect is passed through unchanged" {
        val effect = arrayOf(
            RgbColor(255u, 0u, 0u),
            RgbColorPresets.blank(),
            RgbColor(0u, 0u, 255u)
        )

        val result = blend(BlendMode.Layer, 3, listOf(effect))

        result.size shouldBe 3
        result[0] shouldBe RgbColor(255u, 0u, 0u)
        result[1] shouldBe RgbColorPresets.blank()
        result[2] shouldBe RgbColor(0u, 0u, 255u)
    }
})
