package io.cyborgsquirrel.engine.settings

import io.cyborgsquirrel.engine.enums.BlendMode
import io.cyborgsquirrel.model.strip.LedStrip

data class LedStripRenderSettings(val ledStrip: LedStrip, var blendMode: BlendMode)
