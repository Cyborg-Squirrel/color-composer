package io.cyborgsquirrel.led_strips.requests

import io.cyborgsquirrel.lighting.enums.BlendMode

data class CreateLedStripRequest(
    val name: String,
    val length: Int,
    val height: Int?,
    val powerLimit: Int?,
    val blendMode: BlendMode?
)