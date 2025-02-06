package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effects.settings.SpectrumLightEffectSettings
import io.cyborgsquirrel.model.color.RgbColor

class SpectrumLightEffect(
    numberOfLeds: Int,
    settings: SpectrumLightEffectSettings
) : AnimatedSpectrumLightEffect(numberOfLeds, settings) {

    override fun getName(): String {
        return NAME
    }

    override fun getNextStep(): List<RgbColor> {
        return referenceFrame.ifEmpty {
            super.getNextStep()
        }
    }

    override fun getIterations(): Int {
        // This effect doesn't have a repeating pattern
        return 1
    }

    override fun complete() {
        TODO("Not yet implemented")
    }

    override fun done(): Boolean {
        TODO("Not yet implemented")
    }

    companion object {
        private const val NAME = "Spectrum"
    }
}