package io.cyborgsquirrel.lighting.rendering.filters

import io.cyborgsquirrel.lighting.model.RgbColor
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.util.*

class ReverseFilterTest : BehaviorSpec({

    given("A reverse filter") {
        val filter = ReverseFilter(UUID.randomUUID().toString())
        and("A list of RgbColors") {
            val rgbList = listOf(RgbColor.Green, RgbColor.Blue, RgbColor.Purple, RgbColor.Blank, RgbColor.Blank)
            `when`("The filter is applied to a list of RgbColors") {
                val reflectedList = filter.apply(rgbList)
                then("The filter shall return an identical length list with the elements reversed") {
                    val expectedReversedList = rgbList.reversed()
                    reflectedList.size shouldBe rgbList.size
                    for (i in rgbList.indices) {
                        reflectedList[i] shouldBe expectedReversedList[i]
                    }
                }
            }
        }
    }
})