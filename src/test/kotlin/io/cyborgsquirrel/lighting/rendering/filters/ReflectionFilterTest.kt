package io.cyborgsquirrel.lighting.rendering.filters

import io.cyborgsquirrel.lighting.enums.ReflectionType
import io.cyborgsquirrel.model.color.RgbColor
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ReflectionFilterTest : BehaviorSpec({

    given("A reflection filter configured with reflection low to high") {
        val filter = ReflectionFilter(ReflectionType.LowToHigh)
        and("A list of RgbColors") {
            val rgbList = listOf(RgbColor.Green, RgbColor.Blue, RgbColor.Purple, RgbColor.Blank, RgbColor.Blank)
            `when`("The filter is applied to a list of RgbColors") {
                val reflectedList = filter.apply(rgbList)
                then("The filter shall return an identical length list with the lowest half reflected over the upper half") {
                    val expectedReflectedList =
                        listOf(RgbColor.Green, RgbColor.Blue, RgbColor.Purple, RgbColor.Blue, RgbColor.Green)
                    reflectedList.size shouldBe rgbList.size
                    for (i in rgbList.indices) {
                        reflectedList[i] shouldBe expectedReflectedList[i]
                    }
                }
            }
        }
    }

    given("A reflection filter configured with reflection high to low") {
        val filter = ReflectionFilter(ReflectionType.HighToLow)
        and("A list of RgbColors") {
            val rgbList = listOf(RgbColor.Blank, RgbColor.Blue, RgbColor.Red, RgbColor.Orange)
            `when`("The filter is applied to a list of RgbColors") {
                val reflectedList = filter.apply(rgbList)
                then("The filter shall return an identical length list with the upper half reflected over the lower half") {
                    val expectedReflectedList = listOf(RgbColor.Orange, RgbColor.Red, RgbColor.Red, RgbColor.Orange)
                    reflectedList.size shouldBe rgbList.size
                    for (i in rgbList.indices) {
                        reflectedList[i] shouldBe expectedReflectedList[i]
                    }
                }
            }
        }
    }
})