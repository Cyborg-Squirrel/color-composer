package io.cyborgsquirrel.lighting.palette

import io.cyborgsquirrel.lighting.effect_palette.palette.ChangingColorPalette
import io.cyborgsquirrel.lighting.effect_palette.settings.ChangingGradientPaletteSettings
import io.cyborgsquirrel.lighting.effect_palette.settings.SettingsPalette
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.util.time.TimeHelper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import java.time.Duration

class ChangingColorPaletteTest : StringSpec({

    // Hot-cold gradient from user test data:
    //   gradient 1: red (#FF0000) → yellow (#FFFF00)
    //   gradient 2: green (#00FF00) → blue (#0000FF)
    val gradient1 = mapOf(
        0 to SettingsPalette(RgbColor(255u, 0u, 0u), RgbColor(255u, 0u, 0u), null, emptyList()),
        100 to SettingsPalette(RgbColor(255u, 255u, 0u), RgbColor(255u, 255u, 0u), null, emptyList())
    )
    val gradient2 = mapOf(
        0 to SettingsPalette(RgbColor(0u, 255u, 0u), RgbColor(0u, 255u, 0u), null, emptyList()),
        100 to SettingsPalette(RgbColor(0u, 0u, 255u), RgbColor(0u, 0u, 255u), null, emptyList())
    )
    val settings = ChangingGradientPaletteSettings(
        gradientList = listOf(gradient1, gradient2),
        holdTime = Duration.ofMillis(60_000),
        transitionTime = Duration.ofMillis(5_000),
    )

    // 2 LEDs: index 0 = 0% into gradient, index 1 = 50% into gradient
    val numberOfLeds = 2

    fun timeHelperAt(millis: Long): Pair<TimeHelper, () -> Long> {
        var currentTime = millis
        val mock = mockk<TimeHelper>()
        every { mock.millisSinceEpoch() } answers { currentTime }
        return mock to { currentTime }
    }

    fun mutableTimeHelper(): Pair<TimeHelper, (Long) -> Unit> {
        var currentTime = 0L
        val mock = mockk<TimeHelper>()
        every { mock.millisSinceEpoch() } answers { currentTime }
        return mock to { t: Long -> currentTime = t }
    }

    "hold shows gradient 1: LED 0 is red, LED 1 is orange (not uniform)" {
        val (timeHelper, setTime) = mutableTimeHelper()
        setTime(1_000L)
        val palette = ChangingColorPalette(settings, timeHelper, "test", numberOfLeds)

        // LED 0 = 0% into gradient → pure red
        palette.getPrimaryColor(0) shouldBe RgbColor(255u, 0u, 0u)
        // LED 1 = 50% into gradient → midpoint(#FF0000, #FFFF00)
        palette.getPrimaryColor(1) shouldBe RgbColor(255u, 127u, 0u)
        // Confirm they differ — gradient is not collapsed to a single color
        palette.getPrimaryColor(0) shouldNotBe palette.getPrimaryColor(1)
    }

    "gradient colors remain stable across repeated queries (mutation regression)" {
        val (timeHelper, setTime) = mutableTimeHelper()
        setTime(1_000L)
        val palette = ChangingColorPalette(settings, timeHelper, "test", numberOfLeds)

        val first0 = palette.getPrimaryColor(0)
        val first1 = palette.getPrimaryColor(1)
        // Querying again must not corrupt the stored gradient colors
        val second0 = palette.getPrimaryColor(0)
        val second1 = palette.getPrimaryColor(1)

        second0 shouldBe first0
        second1 shouldBe first1
    }

    "transition blends gradient 1 and gradient 2 colors per LED index" {
        val (timeHelper, setTime) = mutableTimeHelper()
        setTime(1_000L)
        val palette = ChangingColorPalette(settings, timeHelper, "test", numberOfLeds)

        // Expire the hold → ColorHold becomes StartTransition
        setTime(60_001L)
        palette.getPrimaryColor(0)

        // StartTransition → Transition (transitionStart = 60_001)
        palette.getPrimaryColor(0)

        // 50% into the 5 000 ms transition → t = 60_001 + 2_500 = 62_501
        setTime(62_501L)

        // LED 0: blend(#FF0000, #00FF00, 0.5) = #7F7F00
        palette.getPrimaryColor(0) shouldBe RgbColor(127u, 127u, 0u)
        // LED 1: blend(#FF7F00, #007F7F, 0.5) = #7F7F3F
        palette.getPrimaryColor(1) shouldBe RgbColor(127u, 127u, 63u)
        // Colors must still differ by position during transition
        palette.getPrimaryColor(0) shouldNotBe palette.getPrimaryColor(1)
    }

    "after transitioning, hold shows gradient 2: LED 0 is green, LED 1 is teal" {
        val (timeHelper, setTime) = mutableTimeHelper()
        setTime(1_000L)
        val palette = ChangingColorPalette(settings, timeHelper, "test", numberOfLeds)

        // Drive through hold → StartTransition → Transition
        setTime(60_001L)
        palette.getPrimaryColor(0) // ColorHold → StartTransition
        palette.getPrimaryColor(0) // StartTransition → Transition (transitionStart = 60_001)

        // Complete the transition: need now − transitionStart (60_001) > 5_000 → use 65_002
        setTime(65_002L)
        palette.getPrimaryColor(0) // Transition → StartColorHold, counter advances to gradient 2
        palette.getPrimaryColor(0) // StartColorHold → ColorHold

        // Now holding gradient 2
        // LED 0 = 0% → pure green
        palette.getPrimaryColor(0) shouldBe RgbColor(0u, 255u, 0u)
        // LED 1 = 50% → midpoint(#00FF00, #0000FF)
        palette.getPrimaryColor(1) shouldBe RgbColor(0u, 127u, 127u)
        // Gradient must not be uniform
        palette.getPrimaryColor(0) shouldNotBe palette.getPrimaryColor(1)
    }
})
