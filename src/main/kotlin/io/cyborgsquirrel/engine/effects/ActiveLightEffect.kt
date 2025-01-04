package io.cyborgsquirrel.engine.effects

import io.cyborgsquirrel.engine.enums.LightEffectStatus
import io.cyborgsquirrel.model.strip.LedStrip

data class ActiveLightEffect(
    val instanceUuid: String,
    var priority: Int,
    var status: LightEffectStatus,
    val effect: LightEffect,
    val strip: LedStrip
)