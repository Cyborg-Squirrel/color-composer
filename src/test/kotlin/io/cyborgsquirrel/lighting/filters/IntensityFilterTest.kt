package io.cyborgsquirrel.lighting.filters

import io.cyborgsquirrel.lighting.filters.settings.IntensityFilterSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.RgbColorPresets
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.util.*

class IntensityFilterTest : BehaviorSpec({

    given("An intensity filter set to 0.5") {
        val filter = IntensityFilter(IntensityFilterSettings(0.5f), UUID.randomUUID().toString())
        and("An array of RgbColors") {
            `when`("The filter is applied") {
                val buffer = arrayOf(RgbColorPresets.red(), RgbColorPresets.green(), RgbColorPresets.blue())
                val result = filter.apply(buffer)
                then("The filter returns a same-length buffer dimmed by a factor of 0.5") {
                    val expected = arrayOf(
                        RgbColorPresets.red().scale(0.5f),
                        RgbColorPresets.green().scale(0.5f),
                        RgbColorPresets.blue().scale(0.5f),
                    )
                    result.size shouldBe expected.size
                    for (i in expected.indices) {
                        result[i] shouldBe expected[i]
                    }
                }
            }
        }
    }

    given("An intensity filter set to 0.0") {
        val filter = IntensityFilter(IntensityFilterSettings(0f), UUID.randomUUID().toString())
        `when`("The filter is applied") {
            val buffer = arrayOf(RgbColorPresets.red(), RgbColorPresets.white(), RgbColorPresets.blue())
            val result = filter.apply(buffer)
            then("Every color is blanked out") {
                for (color in result) {
                    color shouldBe RgbColorPresets.blank()
                }
            }
        }
    }

    given("An intensity filter set to 2.0") {
        val filter = IntensityFilter(IntensityFilterSettings(2f), UUID.randomUUID().toString())
        `when`("The filter is applied") {
            // amber = (255, 160, 0); doubling brightens and clamps each channel at 255
            val buffer = arrayOf(RgbColorPresets.amber())
            val result = filter.apply(buffer)
            then("Channels are brightened and clamped at 255") {
                result[0] shouldBe RgbColor(255u, 255u, 0u)
            }
        }
    }
})
