package io.cyborgsquirrel.lighting.effects.settings

import io.cyborgsquirrel.model.color.RgbColor

data class NightriderLightEffectSettings(val colorList: List<RgbColor> = RgbColor.Rainbow) : LightEffectSettings