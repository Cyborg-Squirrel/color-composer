package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effects.settings.NightriderLightEffectSettings
import io.cyborgsquirrel.model.color.RgbColor
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.StringSpec

@AnnotationSpec.Test
class NightriderLightEffectTest : StringSpec({

    // Test passes with Kotest in the IDE but not Gradle cmd line, disabled for now.
    "Render nightrider effect".config(enabled = false) {
        val colors = listOf(RgbColor.Red, RgbColor.Blue)
        val length = 6
        val effect = NightriderLightEffect(length, NightriderLightEffectSettings(colors))
        assert(effect.getIterations() == 0)

        var frame = effect.getNextStep()
        assert(frame[0] == RgbColor.Red)
        assert(frame[1] == RgbColor.Red)
        assert(frame[2] == RgbColor.Blank)
        assert(frame[3] == RgbColor.Blank)
        assert(frame[4] == RgbColor.Blank)
        assert(frame[5] == RgbColor.Blank)

        frame = effect.getNextStep()
        assert(frame[0] == RgbColor.Red)
        assert(frame[1] == RgbColor.Red)
        assert(frame[2] == RgbColor.Red)
        assert(frame[3] == RgbColor.Blank)
        assert(frame[4] == RgbColor.Blank)
        assert(frame[5] == RgbColor.Blank)

        frame = effect.getNextStep()
        assert(frame[0] == RgbColor.Red)
        assert(frame[1] == RgbColor.Red)
        assert(frame[2] == RgbColor.Red)
        assert(frame[3] == RgbColor.Red)
        assert(frame[4] == RgbColor.Blank)
        assert(frame[5] == RgbColor.Blank)

        frame = effect.getNextStep()
        assert(frame[0] == RgbColor.Red)
        assert(frame[1] == RgbColor.Red)
        assert(frame[2] == RgbColor.Red)
        assert(frame[3] == RgbColor.Red)
        assert(frame[4] == RgbColor.Red)
        assert(frame[5] == RgbColor.Blank)

        frame = effect.getNextStep()
        assert(frame[0] == RgbColor.Red)
        assert(frame[1] == RgbColor.Red)
        assert(frame[2] == RgbColor.Red)
        assert(frame[3] == RgbColor.Red)
        assert(frame[4] == RgbColor.Red)
        assert(frame[5] == RgbColor.Red)

        frame = effect.getNextStep()
        assert(frame[0] == RgbColor.Red)
        assert(frame[1] == RgbColor.Red)
        assert(frame[2] == RgbColor.Red)
        assert(frame[3] == RgbColor.Red)
        assert(frame[4] == RgbColor.Blue)
        assert(frame[5] == RgbColor.Blue)

        assert(effect.getIterations() == 1)

        frame = effect.getNextStep()
        assert(frame[0] == RgbColor.Red)
        assert(frame[1] == RgbColor.Red)
        assert(frame[2] == RgbColor.Red)
        assert(frame[3] == RgbColor.Blue)
        assert(frame[4] == RgbColor.Blue)
        assert(frame[5] == RgbColor.Blue)

        frame = effect.getNextStep()
        assert(frame[0] == RgbColor.Red)
        assert(frame[1] == RgbColor.Red)
        assert(frame[2] == RgbColor.Blue)
        assert(frame[3] == RgbColor.Blue)
        assert(frame[4] == RgbColor.Blue)
        assert(frame[5] == RgbColor.Blue)

        frame = effect.getNextStep()
        assert(frame[0] == RgbColor.Red)
        assert(frame[1] == RgbColor.Blue)
        assert(frame[2] == RgbColor.Blue)
        assert(frame[3] == RgbColor.Blue)
        assert(frame[4] == RgbColor.Blue)
        assert(frame[5] == RgbColor.Blue)

        frame = effect.getNextStep()
        assert(frame[0] == RgbColor.Blue)
        assert(frame[1] == RgbColor.Blue)
        assert(frame[2] == RgbColor.Blue)
        assert(frame[3] == RgbColor.Blue)
        assert(frame[4] == RgbColor.Blue)
        assert(frame[5] == RgbColor.Blue)

        frame = effect.getNextStep()
        assert(frame[0] == RgbColor.Red)
        assert(frame[1] == RgbColor.Red)
        assert(frame[2] == RgbColor.Blue)
        assert(frame[3] == RgbColor.Blue)
        assert(frame[4] == RgbColor.Blue)
        assert(frame[5] == RgbColor.Blue)

        assert(effect.getIterations() == 2)

        frame = effect.getNextStep()
        assert(frame[0] == RgbColor.Red)
        assert(frame[1] == RgbColor.Red)
        assert(frame[2] == RgbColor.Red)
        assert(frame[3] == RgbColor.Blue)
        assert(frame[4] == RgbColor.Blue)
        assert(frame[5] == RgbColor.Blue)
    }
})