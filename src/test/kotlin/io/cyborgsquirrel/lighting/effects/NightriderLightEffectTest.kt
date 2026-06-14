package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effects.settings.NightriderColorFillEffectSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.RgbColorPresets
import io.cyborgsquirrel.util.time.TimeHelper
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Ignored("Requires more setup with the addition of palettes, fix then remove this ignore flag")
@AnnotationSpec.Test
class NightriderLightEffectTest(val timeHelper: TimeHelper) : StringSpec({

    "Render nightrider effect" {
        val length = 6
        val effect = NightriderLightEffect(length, NightriderColorFillEffectSettings(), null, timeHelper)
        effect.getIterations() shouldBe 0

        var frame = effect.getNextStep()
        frame[0] shouldBe RgbColorPresets.red()
        frame[1] shouldBe RgbColorPresets.red()
        frame[2] shouldBe RgbColorPresets.blank()
        frame[3] shouldBe RgbColorPresets.blank()
        frame[4] shouldBe RgbColorPresets.blank()
        frame[5] shouldBe RgbColorPresets.blank()

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColorPresets.red()
        frame[1] shouldBe RgbColorPresets.red()
        frame[2] shouldBe RgbColorPresets.red()
        frame[3] shouldBe RgbColorPresets.blank()
        frame[4] shouldBe RgbColorPresets.blank()
        frame[5] shouldBe RgbColorPresets.blank()

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColorPresets.red()
        frame[1] shouldBe RgbColorPresets.red()
        frame[2] shouldBe RgbColorPresets.red()
        frame[3] shouldBe RgbColorPresets.red()
        frame[4] shouldBe RgbColorPresets.blank()
        frame[5] shouldBe RgbColorPresets.blank()

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColorPresets.red()
        frame[1] shouldBe RgbColorPresets.red()
        frame[2] shouldBe RgbColorPresets.red()
        frame[3] shouldBe RgbColorPresets.red()
        frame[4] shouldBe RgbColorPresets.red()
        frame[5] shouldBe RgbColorPresets.blank()

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColorPresets.red()
        frame[1] shouldBe RgbColorPresets.red()
        frame[2] shouldBe RgbColorPresets.red()
        frame[3] shouldBe RgbColorPresets.red()
        frame[4] shouldBe RgbColorPresets.red()
        frame[5] shouldBe RgbColorPresets.red()

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColorPresets.red()
        frame[1] shouldBe RgbColorPresets.red()
        frame[2] shouldBe RgbColorPresets.red()
        frame[3] shouldBe RgbColorPresets.red()
        frame[4] shouldBe RgbColorPresets.blue()
        frame[5] shouldBe RgbColorPresets.blue()

        effect.getIterations() shouldBe 1

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColorPresets.red()
        frame[1] shouldBe RgbColorPresets.red()
        frame[2] shouldBe RgbColorPresets.red()
        frame[3] shouldBe RgbColorPresets.blue()
        frame[4] shouldBe RgbColorPresets.blue()
        frame[5] shouldBe RgbColorPresets.blue()

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColorPresets.red()
        frame[1] shouldBe RgbColorPresets.red()
        frame[2] shouldBe RgbColorPresets.blue()
        frame[3] shouldBe RgbColorPresets.blue()
        frame[4] shouldBe RgbColorPresets.blue()
        frame[5] shouldBe RgbColorPresets.blue()

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColorPresets.red()
        frame[1] shouldBe RgbColorPresets.blue()
        frame[2] shouldBe RgbColorPresets.blue()
        frame[3] shouldBe RgbColorPresets.blue()
        frame[4] shouldBe RgbColorPresets.blue()
        frame[5] shouldBe RgbColorPresets.blue()

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColorPresets.blue()
        frame[1] shouldBe RgbColorPresets.blue()
        frame[2] shouldBe RgbColorPresets.blue()
        frame[3] shouldBe RgbColorPresets.blue()
        frame[4] shouldBe RgbColorPresets.blue()
        frame[5] shouldBe RgbColorPresets.blue()

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColorPresets.red()
        frame[1] shouldBe RgbColorPresets.red()
        frame[2] shouldBe RgbColorPresets.blue()
        frame[3] shouldBe RgbColorPresets.blue()
        frame[4] shouldBe RgbColorPresets.blue()
        frame[5] shouldBe RgbColorPresets.blue()

        effect.getIterations() shouldBe 2

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColorPresets.red()
        frame[1] shouldBe RgbColorPresets.red()
        frame[2] shouldBe RgbColorPresets.red()
        frame[3] shouldBe RgbColorPresets.blue()
        frame[4] shouldBe RgbColorPresets.blue()
        frame[5] shouldBe RgbColorPresets.blue()
    }
})
