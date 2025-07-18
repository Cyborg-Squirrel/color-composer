package io.cyborgsquirrel.clients.config

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class PiClientConfig(val id: String, val pin: String, val leds: Int, val brightness: Int)