package io.cyborgsquirrel.lighting.filters

import io.cyborgsquirrel.lighting.filters.settings.IntensityFilterSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.util.*

@AnnotationSpec.Test
class IntensityFilterTest : BehaviorSpec({

    given("An intensity filter set to 0.5") {
        val filter = IntensityFilter(IntensityFilterSettings(0.5f), UUID.randomUUID().toString())
        and("A list of RgbColors") {
            val rgbList = listOf(RgbColor.Red, RgbColor.Green, RgbColor.Blue)
            `when`("The filter is applied to a list of RgbColors") {
                val intensityReducedList = filter.apply(rgbList)
                then("The filter shall return an identical length list dimmed by a factor of 0.5") {
                    intensityReducedList.size shouldBe rgbList.size
                    for (i in rgbList.indices) {
                        intensityReducedList[i] shouldBe rgbList[i].scale(0.5f)
                    }
                }
            }
        }
    }
})