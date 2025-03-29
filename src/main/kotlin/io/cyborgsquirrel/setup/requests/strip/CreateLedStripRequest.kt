package io.cyborgsquirrel.setup.requests.strip

import io.cyborgsquirrel.lighting.enums.BlendMode

data class CreateLedStripRequest(
    val name: String,
    val length: Int,
    val height: Int?,
    val powerLimit: Int?,
    val blendMode: BlendMode?
)