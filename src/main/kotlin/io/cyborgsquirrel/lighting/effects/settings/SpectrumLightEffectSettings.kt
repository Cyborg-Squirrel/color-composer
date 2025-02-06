package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.model.color.RgbColor

data class SpectrumLightEffectSettings(val colorPixelWidth: Int, val colorList: List<RgbColor> = RgbColor.Rainbow) :
    LightEffectSettings