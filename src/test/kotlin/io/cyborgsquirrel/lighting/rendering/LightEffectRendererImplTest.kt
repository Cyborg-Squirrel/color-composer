package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.LightEffect
import io.cyborgsquirrel.lighting.effects.service.LightEffectRegistry
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.EffectLengthMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.SingleLedStripModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class LightEffectRendererImplTest : StringSpec({

    fun singleStrip(uuid: String, length: Int) = SingleLedStripModel(
        name = "strip",
        uuid = uuid,
        pin = "0",
        length = length,
        height = 1,
        blendMode = BlendMode.Layer,
        brightness = 100,
        clientUuid = "client-1",
        inverted = false,
    )

    fun activeEffect(
        uuid: String,
        effect: LightEffect,
        strip: SingleLedStripModel,
        status: LightEffectStatus = LightEffectStatus.Playing,
    ) = ActiveLightEffect(uuid, 0, false, status, effect, emptyList(), strip)

    "never advances a paused effect or asks whether it is due" {
        val strip = singleStrip("strip-2", 3)
        val registry = mockk<LightEffectRegistry>()
        val effect = mockk<LightEffect>()
        val currentBuf = listOf(RgbColor(5u, 5u, 5u), RgbColor.Blank, RgbColor.Blank)
        every { effect.getBuffer() } returns currentBuf
        every { registry.getAllEffectsForStrip("strip-2") } returns
            listOf(activeEffect("a", effect, strip, LightEffectStatus.Paused))

        val renderer = LightEffectRendererImpl(registry)
        val result = renderer.renderFrames(listOf(strip), "client-1")

        result[0].frameData shouldBe currentBuf
        verify(exactly = 0) { effect.getNextStep() }
    }

    "Truncate mode truncates effect output longer than the strip" {
        val strip = singleStrip("strip-trunc", 3)
        val registry = mockk<LightEffectRegistry>()
        val effect = mockk<LightEffect>()
        // Effect renders more LEDs than the strip has (e.g. a marquee with an oversized dot buffer).
        val oversized = listOf(
            RgbColor(1u, 0u, 0u), RgbColor(2u, 0u, 0u), RgbColor(3u, 0u, 0u),
            RgbColor(4u, 0u, 0u), RgbColor(5u, 0u, 0u)
        )
        every { effect.getNextStep() } returns oversized
        every { registry.getAllEffectsForStrip("strip-trunc") } returns
            listOf(activeEffect("a", effect, strip))

        val renderer = LightEffectRendererImpl(registry)
        val result = renderer.renderFrames(listOf(strip), "client-1", EffectLengthMode.Truncate)

        result[0].frameData shouldBe oversized.take(3)
    }

    "Truncate mode leaves output matching the strip length untouched" {
        val strip = singleStrip("strip-trunc-eq", 3)
        val registry = mockk<LightEffectRegistry>()
        val effect = mockk<LightEffect>()
        val exact = listOf(RgbColor(1u, 0u, 0u), RgbColor(2u, 0u, 0u), RgbColor(3u, 0u, 0u))
        every { effect.getNextStep() } returns exact
        every { registry.getAllEffectsForStrip("strip-trunc-eq") } returns
            listOf(activeEffect("a", effect, strip))

        val renderer = LightEffectRendererImpl(registry)
        val result = renderer.renderFrames(listOf(strip), "client-1", EffectLengthMode.Truncate)

        result[0].frameData shouldBe exact
    }

    "Ignore mode drops effects whose output does not equal the strip length" {
        val strip = singleStrip("strip-ignore", 3)
        val registry = mockk<LightEffectRegistry>()
        val effect = mockk<LightEffect>()
        val mismatched = listOf(
            RgbColor(1u, 0u, 0u), RgbColor(2u, 0u, 0u), RgbColor(3u, 0u, 0u), RgbColor(4u, 0u, 0u)
        )
        every { effect.getNextStep() } returns mismatched
        every { registry.getAllEffectsForStrip("strip-ignore") } returns
            listOf(activeEffect("a", effect, strip))

        val renderer = LightEffectRendererImpl(registry)
        val result = renderer.renderFrames(listOf(strip), "client-1", EffectLengthMode.Ignore)

        // The lone effect was ignored, so the blended frame is strip-length and blank.
        result[0].frameData.size shouldBe 3
        result[0].frameData.all { it.isBlank() } shouldBe true
    }

    "Ignore mode keeps effects whose output equals the strip length" {
        val strip = singleStrip("strip-ignore-eq", 3)
        val registry = mockk<LightEffectRegistry>()
        val effect = mockk<LightEffect>()
        val exact = listOf(RgbColor(1u, 0u, 0u), RgbColor(2u, 0u, 0u), RgbColor(3u, 0u, 0u))
        every { effect.getNextStep() } returns exact
        every { registry.getAllEffectsForStrip("strip-ignore-eq") } returns
            listOf(activeEffect("a", effect, strip))

        val renderer = LightEffectRendererImpl(registry)
        val result = renderer.renderFrames(listOf(strip), "client-1", EffectLengthMode.Ignore)

        result[0].frameData shouldBe exact
    }

    "Permissive mode allows output that does not match the strip length" {
        val strip = singleStrip("strip-permissive", 3)
        val registry = mockk<LightEffectRegistry>()
        val effect = mockk<LightEffect>()
        val oversized = listOf(
            RgbColor(1u, 0u, 0u), RgbColor(2u, 0u, 0u), RgbColor(3u, 0u, 0u),
            RgbColor(4u, 0u, 0u), RgbColor(5u, 0u, 0u)
        )
        every { effect.getNextStep() } returns oversized
        every { registry.getAllEffectsForStrip("strip-permissive") } returns
            listOf(activeEffect("a", effect, strip))

        val renderer = LightEffectRendererImpl(registry)
        val result = renderer.renderFrames(listOf(strip), "client-1", EffectLengthMode.Permissive)

        result[0].frameData shouldBe oversized
    }
})
