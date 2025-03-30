package io.cyborgsquirrel.led_strips.responses

import io.cyborgsquirrel.lighting.enums.BlendMode

data class LedStripResponse(
    val clientUuid: String,
    val name: String,
    val uuid: String,
    val length: Int,
    val height: Int,
    val powerLimit: Int?,
    val blendMode: BlendMode
)