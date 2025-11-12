package io.cyborgsquirrel.led_strips.responses

import io.cyborgsquirrel.lighting.enums.BlendMode
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetLedStripResponse(
    val clientUuid: String,
    val name: String,
    val uuid: String,
    val pin: String,
    val length: Int,
    val height: Int,
    val powerLimit: Int?,
    val brightness: Int,
    val blendMode: BlendMode,
    val activeEffects: Int,
)