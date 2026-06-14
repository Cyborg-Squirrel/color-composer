package io.cyborgsquirrel.lighting.filters

import io.cyborgsquirrel.lighting.enums.ReflectionType
import io.cyborgsquirrel.lighting.filters.settings.ReflectionFilterSettings
import io.cyborgsquirrel.lighting.model.RgbColorPresets
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.util.*

class ReflectionFilterTest : BehaviorSpec({

    given("A reflection filter configured low to high") {
        val filter = ReflectionFilter(ReflectionFilterSettings(ReflectionType.LowToHigh), UUID.randomUUID().toString())
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
                then("The lower half is mirrored over the upper half about the center") {
                    val expected = arrayOf(
                        RgbColorPresets.green(),
                        RgbColorPresets.blue(),
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

    given("A reflection filter configured high to low") {
        val filter = ReflectionFilter(ReflectionFilterSettings(ReflectionType.HighToLow), UUID.randomUUID().toString())
        and("An array of RgbColors") {
            `when`("The filter is applied") {
                val buffer = arrayOf(
                    RgbColorPresets.blank(),
                    RgbColorPresets.blue(),
                    RgbColorPresets.red(),
                    RgbColorPresets.orange(),
                )
                val result = filter.apply(buffer)
                then("The upper half is mirrored over the lower half") {
                    val expected = arrayOf(
                        RgbColorPresets.orange(),
                        RgbColorPresets.red(),
                        RgbColorPresets.red(),
                        RgbColorPresets.orange(),
                    )
                    result.size shouldBe expected.size
                    for (i in expected.indices) {
                        result[i] shouldBe expected[i]
                    }
                }
            }
        }
    }

    given("A reflection filter configured to copy over center") {
        val filter =
            ReflectionFilter(ReflectionFilterSettings(ReflectionType.CopyOverCenter), UUID.randomUUID().toString())
        and("An array whose upper half is blank") {
            `when`("The filter is applied") {
                val buffer = arrayOf(
                    RgbColorPresets.green(),
                    RgbColorPresets.blue(),
                    RgbColorPresets.purple(),
                    RgbColorPresets.blank(),
                    RgbColorPresets.blank(),
                )
                val result = filter.apply(buffer)
                then("Blank slots are filled with the mirror of their counterpart") {
                    val expected = arrayOf(
                        RgbColorPresets.green(),
                        RgbColorPresets.blue(),
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

        and("An array with no blank slots") {
            `when`("The filter is applied") {
                val buffer = arrayOf(
                    RgbColorPresets.green(),
                    RgbColorPresets.blue(),
                    RgbColorPresets.purple(),
                    RgbColorPresets.red(),
                    RgbColorPresets.white(),
                )
                val result = filter.apply(buffer)
                then("Non-blank destinations are left untouched") {
                    val expected = arrayOf(
                        RgbColorPresets.green(),
                        RgbColorPresets.blue(),
                        RgbColorPresets.purple(),
                        RgbColorPresets.red(),
                        RgbColorPresets.white(),
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
