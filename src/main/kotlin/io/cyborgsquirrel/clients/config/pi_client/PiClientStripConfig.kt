package io.cyborgsquirrel.clients.config.pi_client

import com.fasterxml.jackson.annotation.JsonProperty
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class PiClientStripConfig(
    val uuid: String,
    val pin: String,
    val leds: Int,
    val brightness: Int,
    @JsonProperty("color_order")
    val colorOrder: ColorOrder
)