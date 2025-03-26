package io.cyborgsquirrel.lighting.rendering.filters

import io.cyborgsquirrel.lighting.rendering.filters.settings.BrightnessFilterSettings
import io.cyborgsquirrel.model.color.RgbColor
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.util.*

@AnnotationSpec.Test
class BrightnessFilterTest : BehaviorSpec({

    given("A brightness filter set to 0.5") {
        val filter = BrightnessFilter(BrightnessFilterSettings(0.5f), UUID.randomUUID().toString())
        and("A list of RgbColors") {
            val rgbList = listOf(RgbColor.Red, RgbColor.Green, RgbColor.Blue)
            `when`("The filter is applied to a list of RgbColors") {
                val brightnessReducedList = filter.apply(rgbList)
                then("The filter shall return an identical length list dimmed by a factor of 0.5") {
                    brightnessReducedList.size shouldBe rgbList.size
                    for (i in rgbList.indices) {
                        brightnessReducedList[i] shouldBe rgbList[i].scale(0.5f)
                    }
                }
            }
        }
    }
})