package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effects.settings.MarqueeEffectSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.RgbColorPresets
import io.cyborgsquirrel.util.time.TimeHelper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class MarqueeEffectTest : StringSpec({

    // The effect builds an internal "dot" buffer that is larger than numberOfLeds so the
    // scrolling pattern tiles seamlessly at the end of the strip. These tests make sure the
    // *rendered* output (and the cached buffer) is always exactly numberOfLeds regardless of
    // how the dot/space lengths line up against the strip length.

    /**
     * TimeHelper that advances by [stepMillis] on every call so each getNextStep() is considered
     * "due" and renders a fresh frame.
     */
    fun advancingTimeHelper(stepMillis: Long = 1000): TimeHelper {
        val mockTimeHelper = mockk<TimeHelper>()
        var current = 0L
        every { mockTimeHelper.millisSinceEpoch() } answers {
            current += stepMillis
            current
        }
        return mockTimeHelper
    }

    /**
     * TimeHelper frozen at [millis] so that only the first getNextStep() is "due".
     */
    fun fixedTimeHelper(millis: Long): TimeHelper {
        val mockTimeHelper = mockk<TimeHelper>()
        every { mockTimeHelper.millisSinceEpoch() } returns millis
        return mockTimeHelper
    }

    "Rendered frame size always equals numberOfLeds" {
        // Mix of settings where the pattern period divides the strip evenly and where it does not,
        // plus strips both shorter and longer than a single dot+space period.
        val settingsList = listOf(
            MarqueeEffectSettings(dotLength = 2, spaceBetweenDots = 2),
            MarqueeEffectSettings(dotLength = 3, spaceBetweenDots = 2),
            MarqueeEffectSettings(dotLength = 1, spaceBetweenDots = 5),
            MarqueeEffectSettings(dotLength = 5, spaceBetweenDots = 1),
            MarqueeEffectSettings(dotLength = 4, spaceBetweenDots = 4),
        )
        val ledCounts = listOf(1, 2, 3, 6, 7, 10, 13, 30, 31)

        for (settings in settingsList) {
            for (numberOfLeds in ledCounts) {
                val effect = MarqueeEffect(numberOfLeds, settings, null, advancingTimeHelper())

                // Advance through more than a full scroll cycle (shiftAmount wraps every
                // numberOfLeds frames) to exercise the wrap-around at the end of the strip.
                repeat(numberOfLeds * 2 + 3) {
                    val frame = effect.getNextStep()
                    frame.size shouldBe numberOfLeds
                    effect.getBuffer().size shouldBe numberOfLeds
                }
            }
        }
    }

    "Buffer is numberOfLeds before any frame is rendered" {
        val numberOfLeds = 12
        val effect = MarqueeEffect(numberOfLeds, MarqueeEffectSettings(), null, advancingTimeHelper())

        effect.getBuffer().size shouldBe numberOfLeds
        effect.getBuffer().all { it == RgbColorPresets.blank() } shouldBe true
    }

    "getBuffer matches the last rendered frame" {
        val numberOfLeds = 9
        val effect = MarqueeEffect(numberOfLeds, MarqueeEffectSettings(), null, advancingTimeHelper())

        repeat(5) {
            val frame = effect.getNextStep()
            effect.getBuffer() shouldBe frame
        }
    }

    "Renders expected dot pattern for default settings" {
        // dotLength=2, spaceBetweenDots=2 over 6 LEDs. The internal dot buffer is [T,T,F,F,T,T,F,F]
        // (length 8 > numberOfLeds) and shiftAmount starts at 1 on the first rendered frame.
        val numberOfLeds = 6
        val effect = MarqueeEffect(numberOfLeds, MarqueeEffectSettings(), null, advancingTimeHelper())

        val c = RgbColorPresets.cyan()
        val b = RgbColorPresets.blank()

        var frame = effect.getNextStep()
        frame shouldBe arrayOf(c, b, b, c, c, b)

        frame = effect.getNextStep()
        frame shouldBe arrayOf(b, b, c, c, b, b)
    }

    "Returns cached buffer when an update is not yet due" {
        val numberOfLeds = 6
        val effect = MarqueeEffect(numberOfLeds, MarqueeEffectSettings(), null, fixedTimeHelper(10_000))

        // First call is due and renders a frame.
        val firstFrame = effect.getNextStep()
        firstFrame.size shouldBe numberOfLeds

        // Time has not advanced past the per-update interval, so the same buffer comes back
        // unchanged (the pattern does not scroll).
        val secondFrame = effect.getNextStep()
        secondFrame shouldBe firstFrame
        secondFrame.size shouldBe numberOfLeds
        effect.getBuffer() shouldBe firstFrame
    }
})
