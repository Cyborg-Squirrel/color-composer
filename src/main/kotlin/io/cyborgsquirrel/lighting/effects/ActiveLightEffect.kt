package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.rendering.filters.LightEffectFilter
import io.cyborgsquirrel.model.strip.LedStrip

data class ActiveLightEffect(
    val uuid: String,
    val priority: Int,
    val skipFramesIfBlank: Boolean,
    val status: LightEffectStatus,
    val effect: LightEffect,
    val strip: LedStrip,
    val filters: List<LightEffectFilter>
)