package io.cyborgsquirrel.led_strips.requests

import io.cyborgsquirrel.lighting.enums.BlendMode
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class UpdateLedStripRequest(
    val name: String?,
    val pin: String?,
    val length: Int?,
    val height: Int?,
    val brightness: Int?,
    val blendMode: BlendMode?,
    val clientUuid: String?,
)