package io.cyborgsquirrel.led_strips.requests

import io.cyborgsquirrel.lighting.enums.BlendMode
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class CreateLedStripRequest(
    val clientUuid: String,
    val name: String,
    val length: Int,
    val height: Int? = null,
    val powerLimit: Int? = null,
    val blendMode: BlendMode?
)