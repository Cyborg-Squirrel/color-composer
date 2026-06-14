package io.cyborgsquirrel.lighting.filters

import io.cyborgsquirrel.lighting.model.RgbColorPresets
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.util.*

class ReverseFilterTest : BehaviorSpec({

    given("A reverse filter") {
        val filter = ReverseFilter(UUID.randomUUID().toString())
        and("An array of RgbColors") {
            `when`("The filter is applied") {
                val buffer = arrayOf(
                    RgbColorPresets.green(),
                    RgbColorPresets.blue(),
                    RgbColorPresets.purple(),
                    RgbColorPresets.blank(),
                    RgbColorPresets.blank(),
                )
                val result = filter.apply(buffer)
                then("The filter returns a same-length buffer with the elements reversed") {
                    val expected = arrayOf(
                        RgbColorPresets.blank(),
                        RgbColorPresets.blank(),
                        RgbColorPresets.purple(),
                        RgbColorPresets.blue(),
                        RgbColorPresets.green(),
                    )
                    result.size shouldBe expected.size
                    for (i in expected.indices) {
                        result[i] shouldBe expected[i]
                    }
                }
            }
        }
    }
})
