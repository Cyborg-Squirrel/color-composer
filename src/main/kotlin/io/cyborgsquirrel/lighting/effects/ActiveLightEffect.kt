package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.filters.LightEffectFilter
import io.cyborgsquirrel.lighting.model.LedStrip

data class ActiveLightEffect(
    val effectUuid: String,
    val priority: Int,
    val skipFramesIfBlank: Boolean,
    val status: LightEffectStatus,
    val effect: LightEffect,
    val strip: LedStrip,
    val filters: List<LightEffectFilter>
)