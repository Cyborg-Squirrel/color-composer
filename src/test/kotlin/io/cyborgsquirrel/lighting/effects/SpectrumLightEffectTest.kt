package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effects.settings.SpectrumEffectSettings
import io.cyborgsquirrel.lighting.model.RgbColorPresets
import io.cyborgsquirrel.util.time.TimeHelper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class SpectrumLightEffectTest : StringSpec({

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

    "Buffer is numberOfLeds and blank before any frame is rendered" {
        val numberOfLeds = 12
        val effect = SpectrumLightEffect(numberOfLeds, SpectrumEffectSettings(), null, advancingTimeHelper())

        effect.getBuffer().size shouldBe numberOfLeds
        effect.getBuffer().all { it == RgbColorPresets.blank() } shouldBe true
        effect.getIterations() shouldBe 0
    }

    "First frame lays the rainbow palette out one color per LED when the band is a single LED" {
        // colorBandPercentage 10% of 12 LEDs -> colorWidth 1, so each LED is a discrete palette color and the
        // 6-color rainbow tiles across the strip.
        val numberOfLeds = 12
        val effect = SpectrumLightEffect(numberOfLeds, SpectrumEffectSettings(colorBandPercentage = 10), null, advancingTimeHelper())

        val frame = effect.getNextStep()
        val rainbow = RgbColorPresets.rainbow()
        frame.size shouldBe numberOfLeds
        for (i in 0..<numberOfLeds) {
            frame[i] shouldBe rainbow[i % rainbow.size]
        }
    }

    "First frame fills the entire strip across multiple gradient bands" {
        // colorWidth = 25% of 12 = 3, so there are 4 bands of 3 LEDs. Every LED must be written (no trailing blanks)
        // and each band must start exactly on its base palette color.
        val numberOfLeds = 12
        val effect = SpectrumLightEffect(numberOfLeds, SpectrumEffectSettings(colorBandPercentage = 25), null, advancingTimeHelper())

        val frame = effect.getNextStep()
        val rainbow = RgbColorPresets.rainbow()

        frame.size shouldBe numberOfLeds
        // No LED is left blank: every position belongs to some band.
        frame.any { it == RgbColorPresets.blank() } shouldBe false
        // Band boundaries (every 3rd LED) start on the base color, with interpolation toward the next color in between.
        frame[0] shouldBe rainbow[0]
        frame[3] shouldBe rainbow[1]
        frame[6] shouldBe rainbow[2]
        frame[9] shouldBe rainbow[3]
        // Interior LEDs are interpolated, so they differ from the band's start color.
        (frame[1] == rainbow[0]) shouldBe false
    }

    "Returns the cached buffer unchanged when an update is not yet due" {
        val numberOfLeds = 6
        val effect = SpectrumLightEffect(numberOfLeds, SpectrumEffectSettings(), null, fixedTimeHelper(10_000))

        val firstFrame = effect.getNextStep()
        firstFrame.size shouldBe numberOfLeds

        // Time has not advanced past the per-update interval, so the same buffer comes back unchanged.
        val secondFrame = effect.getNextStep()
        secondFrame shouldBe firstFrame
        effect.getBuffer() shouldBe firstFrame
    }

    "Animated effect scrolls the spectrum left by one LED each frame" {
        // colorWidth 1 (20% of 6 = 1) gives a discrete rainbow that is easy to track as it rotates.
        val numberOfLeds = 6
        val effect = SpectrumLightEffect(
            numberOfLeds,
            SpectrumEffectSettings(colorBandPercentage = 20, animated = true),
            null,
            advancingTimeHelper(),
        )

        val rainbow = RgbColorPresets.rainbow()

        // First frame is the un-shifted reference.
        var frame = effect.getNextStep()
        for (i in 0..<numberOfLeds) frame[i] shouldBe rainbow[i % rainbow.size]

        // Each subsequent frame rotates the pattern left by one (buffer[i] = reference[(i + shift) % n]).
        for (shift in 1..<numberOfLeds) {
            frame = effect.getNextStep()
            for (i in 0..<numberOfLeds) {
                frame[i] shouldBe rainbow[(i + shift) % rainbow.size]
            }
        }

        // After a full cycle the pattern wraps back to the reference and an iteration is recorded.
        effect.getIterations() shouldBe 0
        frame = effect.getNextStep()
        for (i in 0..<numberOfLeds) frame[i] shouldBe rainbow[i % rainbow.size]
        effect.getIterations() shouldBe 1
    }

    "Non-animated effect holds the same static frame" {
        val numberOfLeds = 6
        val effect = SpectrumLightEffect(
            numberOfLeds,
            SpectrumEffectSettings(colorBandPercentage = 20, animated = false),
            null,
            advancingTimeHelper(),
        )

        val firstFrame = effect.getNextStep().copyOf()
        repeat(5) {
            val frame = effect.getNextStep()
            for (i in 0..<numberOfLeds) frame[i] shouldBe firstFrame[i]
        }
    }

    "getBuffer matches the last rendered frame" {
        val numberOfLeds = 9
        val effect = SpectrumLightEffect(
            numberOfLeds,
            SpectrumEffectSettings(colorBandPercentage = 20, animated = true),
            null,
            advancingTimeHelper(),
        )

        repeat(5) {
            val frame = effect.getNextStep()
            effect.getBuffer() shouldBe frame
        }
    }
})
