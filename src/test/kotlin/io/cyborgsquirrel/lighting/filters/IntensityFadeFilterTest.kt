package io.cyborgsquirrel.lighting.filters

import io.cyborgsquirrel.lighting.filters.settings.IntensityFadeFilterSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.util.time.TimeHelper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant

class IntensityFadeFilterTest : StringSpec({

    fun setupTimeHelper(millisSinceEpoch: Long): TimeHelper {
        val mockTimeHelper = mockk<TimeHelper>()
        every { mockTimeHelper.millisSinceEpoch() } returns millisSinceEpoch
        coEvery { mockTimeHelper.millisSinceEpoch() } returnsMany listOf(
            millisSinceEpoch,
            millisSinceEpoch + 1000,
            millisSinceEpoch + 2000
        )
        return mockTimeHelper
    }

    fun createFilter(
        startingIntensity: Float,
        endingIntensity: Float,
        fadeDuration: Duration,
        uuid: String = "test-uuid"
    ): IntensityFadeFilter {
        val millisSinceEpoch = Timestamp.from(Instant.now()).time
        val mockTimeHelper = setupTimeHelper(millisSinceEpoch)
        val settings = IntensityFadeFilterSettings(
            startingIntensity = startingIntensity,
            endingIntensity = endingIntensity,
            fadeDuration = fadeDuration
        )
        return IntensityFadeFilter(settings, mockTimeHelper, uuid)
    }

    fun createColorBuffer(): List<RgbColor> {
        return listOf(
            RgbColor(255u, 0u, 0u),
            RgbColor(0u, 255u, 0u),
            RgbColor(0u, 0u, 255u)
        )
    }

    "IntensityFadeFilter fade in" {
        val filter = createFilter(0.5f, 1.0f, Duration.ofSeconds(2))
        val colorBuffer = createColorBuffer()

        var filteredBuffer = filter.apply(colorBuffer)
        filteredBuffer[0] shouldBe colorBuffer[0].scale(0.5f)
        filteredBuffer[1] shouldBe colorBuffer[1].scale(0.5f)
        filteredBuffer[2] shouldBe colorBuffer[2].scale(0.5f)

        filteredBuffer = filter.apply(colorBuffer)
        filteredBuffer[0] shouldBe colorBuffer[0].scale(0.75f)
        filteredBuffer[1] shouldBe colorBuffer[1].scale(0.75f)
        filteredBuffer[2] shouldBe colorBuffer[2].scale(0.75f)

        filteredBuffer = filter.apply(colorBuffer)
        filteredBuffer[0] shouldBe colorBuffer[0].scale(1.0f)
        filteredBuffer[1] shouldBe colorBuffer[1].scale(1.0f)
        filteredBuffer[2] shouldBe colorBuffer[2].scale(1.0f)
    }

    "IntensityFadeFilter fade out" {
        val filter = createFilter(1.0f, 0f, Duration.ofSeconds(2))
        val colorBuffer = createColorBuffer()

        var filteredBuffer = filter.apply(colorBuffer)
        filteredBuffer[0] shouldBe colorBuffer[0]
        filteredBuffer[1] shouldBe colorBuffer[1]
        filteredBuffer[2] shouldBe colorBuffer[2]

        filteredBuffer = filter.apply(colorBuffer)
        filteredBuffer[0] shouldBe colorBuffer[0].scale(0.5f)
        filteredBuffer[1] shouldBe colorBuffer[1].scale(0.5f)
        filteredBuffer[2] shouldBe colorBuffer[2].scale(0.5f)

        filteredBuffer = filter.apply(colorBuffer)
        filteredBuffer[0] shouldBe colorBuffer[0].scale(0f)
        filteredBuffer[1] shouldBe colorBuffer[1].scale(0f)
        filteredBuffer[2] shouldBe colorBuffer[2].scale(0f)
    }

    "IntensityFadeFilter with zero duration" {
        val filter = createFilter(0.5f, 1.0f, Duration.ofSeconds(0))
        val colorBuffer = createColorBuffer()

        var filteredBuffer = filter.apply(colorBuffer)
        filteredBuffer[0] shouldBe colorBuffer[0].scale(1.0f)
        filteredBuffer[1] shouldBe colorBuffer[1].scale(1.0f)
        filteredBuffer[2] shouldBe colorBuffer[2].scale(1.0f)

        filteredBuffer = filter.apply(colorBuffer)
        filteredBuffer[0] shouldBe colorBuffer[0].scale(1.0f)
        filteredBuffer[1] shouldBe colorBuffer[1].scale(1.0f)
        filteredBuffer[2] shouldBe colorBuffer[2].scale(1.0f)
    }
})