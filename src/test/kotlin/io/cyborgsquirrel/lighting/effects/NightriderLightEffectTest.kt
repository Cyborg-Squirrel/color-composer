package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effects.settings.NightriderEffectSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Ignored("Requires more setup with the addition of palettes, fix then remove this ignore flag")
@AnnotationSpec.Test
class NightriderLightEffectTest : StringSpec({

    "Render nightrider effect" {
        val length = 6
        val effect = NightriderLightEffect(length, NightriderEffectSettings.default(), null)
        effect.getIterations() shouldBe 0

        var frame = effect.getNextStep()
        frame[0] shouldBe RgbColor.Red
        frame[1] shouldBe RgbColor.Red
        frame[2] shouldBe RgbColor.Blank
        frame[3] shouldBe RgbColor.Blank
        frame[4] shouldBe RgbColor.Blank
        frame[5] shouldBe RgbColor.Blank

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColor.Red
        frame[1] shouldBe RgbColor.Red
        frame[2] shouldBe RgbColor.Red
        frame[3] shouldBe RgbColor.Blank
        frame[4] shouldBe RgbColor.Blank
        frame[5] shouldBe RgbColor.Blank

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColor.Red
        frame[1] shouldBe RgbColor.Red
        frame[2] shouldBe RgbColor.Red
        frame[3] shouldBe RgbColor.Red
        frame[4] shouldBe RgbColor.Blank
        frame[5] shouldBe RgbColor.Blank

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColor.Red
        frame[1] shouldBe RgbColor.Red
        frame[2] shouldBe RgbColor.Red
        frame[3] shouldBe RgbColor.Red
        frame[4] shouldBe RgbColor.Red
        frame[5] shouldBe RgbColor.Blank

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColor.Red
        frame[1] shouldBe RgbColor.Red
        frame[2] shouldBe RgbColor.Red
        frame[3] shouldBe RgbColor.Red
        frame[4] shouldBe RgbColor.Red
        frame[5] shouldBe RgbColor.Red

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColor.Red
        frame[1] shouldBe RgbColor.Red
        frame[2] shouldBe RgbColor.Red
        frame[3] shouldBe RgbColor.Red
        frame[4] shouldBe RgbColor.Blue
        frame[5] shouldBe RgbColor.Blue

        effect.getIterations() shouldBe 1

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColor.Red
        frame[1] shouldBe RgbColor.Red
        frame[2] shouldBe RgbColor.Red
        frame[3] shouldBe RgbColor.Blue
        frame[4] shouldBe RgbColor.Blue
        frame[5] shouldBe RgbColor.Blue

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColor.Red
        frame[1] shouldBe RgbColor.Red
        frame[2] shouldBe RgbColor.Blue
        frame[3] shouldBe RgbColor.Blue
        frame[4] shouldBe RgbColor.Blue
        frame[5] shouldBe RgbColor.Blue

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColor.Red
        frame[1] shouldBe RgbColor.Blue
        frame[2] shouldBe RgbColor.Blue
        frame[3] shouldBe RgbColor.Blue
        frame[4] shouldBe RgbColor.Blue
        frame[5] shouldBe RgbColor.Blue

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColor.Blue
        frame[1] shouldBe RgbColor.Blue
        frame[2] shouldBe RgbColor.Blue
        frame[3] shouldBe RgbColor.Blue
        frame[4] shouldBe RgbColor.Blue
        frame[5] shouldBe RgbColor.Blue

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColor.Red
        frame[1] shouldBe RgbColor.Red
        frame[2] shouldBe RgbColor.Blue
        frame[3] shouldBe RgbColor.Blue
        frame[4] shouldBe RgbColor.Blue
        frame[5] shouldBe RgbColor.Blue

        effect.getIterations() shouldBe 2

        frame = effect.getNextStep()
        frame[0] shouldBe RgbColor.Red
        frame[1] shouldBe RgbColor.Red
        frame[2] shouldBe RgbColor.Red
        frame[3] shouldBe RgbColor.Blue
        frame[4] shouldBe RgbColor.Blue
        frame[5] shouldBe RgbColor.Blue
    }
})