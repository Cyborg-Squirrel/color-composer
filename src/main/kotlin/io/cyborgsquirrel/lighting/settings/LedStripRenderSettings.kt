package io.cyborgsquirrel.lighting.settings

import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.model.strip.LedStrip

data class LedStripRenderSettings(val ledStrip: LedStrip, var blendMode: BlendMode)
