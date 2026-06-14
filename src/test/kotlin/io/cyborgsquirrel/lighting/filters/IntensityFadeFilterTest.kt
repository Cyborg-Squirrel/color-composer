package io.cyborgsquirrel.lighting.filters

import io.cyborgsquirrel.lighting.filters.settings.IntensityFadeFilterSettings
import io.cyborgsquirrel.lighting.model.RgbColorPresets
import io.cyborgsquirrel.util.time.TimeHelper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.Duration

class IntensityFadeFilterTest : StringSpec({

    val baseMillis = 1_000_000L

    // The filter calls millisSinceEpoch() once per apply(); supplying the sequence of timestamps lets us
    // drive it deterministically frame by frame.
    fun createFilter(
        startingIntensity: Float,
        endingIntensity: Float,
        fadeDuration: Duration,
        timestamps: List<Long>,
    ): IntensityFadeFilter {
        val mockTimeHelper = mockk<TimeHelper>()
        every { mockTimeHelper.millisSinceEpoch() } returnsMany timestamps
        val settings = IntensityFadeFilterSettings(
            startingIntensity = startingIntensity,
            endingIntensity = endingIntensity,
            fadeDuration = fadeDuration,
        )
        return IntensityFadeFilter(settings, mockTimeHelper, "test-uuid")
    }

    // Fresh instances each call so an in-place apply() never compounds across frames.
    fun colorBuffer() = arrayOf(RgbColorPresets.red(), RgbColorPresets.green(), RgbColorPresets.blue())

    fun scaledBuffer(scaleFactor: Float) = listOf(
        RgbColorPresets.red().scale(scaleFactor),
        RgbColorPresets.green().scale(scaleFactor),
        RgbColorPresets.blue().scale(scaleFactor),
    )

    "fades in from the starting intensity to the ending intensity across the fade duration" {
        val filter = createFilter(
            startingIntensity = 0.5f,
            endingIntensity = 1.0f,
            fadeDuration = Duration.ofSeconds(2),
            timestamps = listOf(baseMillis, baseMillis + 1000, baseMillis + 2000),
        )

        filter.apply(colorBuffer()).toList() shouldBe scaledBuffer(0.5f)
        filter.apply(colorBuffer()).toList() shouldBe scaledBuffer(0.75f)
        filter.apply(colorBuffer()).toList() shouldBe scaledBuffer(1.0f)
    }

    "fades out from the starting intensity to the ending intensity across the fade duration" {
        val filter = createFilter(
            startingIntensity = 1.0f,
            endingIntensity = 0f,
            fadeDuration = Duration.ofSeconds(2),
            timestamps = listOf(baseMillis, baseMillis + 1000, baseMillis + 2000),
        )

        filter.apply(colorBuffer()).toList() shouldBe scaledBuffer(1.0f)
        filter.apply(colorBuffer()).toList() shouldBe scaledBuffer(0.5f)
        filter.apply(colorBuffer()).toList() shouldBe scaledBuffer(0f)
    }

    "with a zero fade duration immediately uses the ending intensity" {
        val filter = createFilter(
            startingIntensity = 0.5f,
            endingIntensity = 1.0f,
            fadeDuration = Duration.ZERO,
            timestamps = listOf(baseMillis, baseMillis + 1000),
        )

        filter.apply(colorBuffer()).toList() shouldBe scaledBuffer(1.0f)
        filter.apply(colorBuffer()).toList() shouldBe scaledBuffer(1.0f)
    }

    "holds the ending intensity once the fade duration has elapsed" {
        val filter = createFilter(
            startingIntensity = 0f,
            endingIntensity = 1.0f,
            fadeDuration = Duration.ofSeconds(2),
            timestamps = listOf(baseMillis, baseMillis + 5000),
        )

        filter.apply(colorBuffer()).toList() shouldBe scaledBuffer(0f)
        filter.apply(colorBuffer()).toList() shouldBe scaledBuffer(1.0f)
    }
})
