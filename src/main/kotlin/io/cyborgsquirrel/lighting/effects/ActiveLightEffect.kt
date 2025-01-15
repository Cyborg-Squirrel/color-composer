package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.model.strip.LedStrip

data class ActiveLightEffect(
    val uuid: String,
    var priority: Int,
    var status: LightEffectStatus,
    val effect: LightEffect,
    val strip: LedStrip
)