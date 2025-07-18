package io.cyborgsquirrel.clients.config.pi_client

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class PiClientConfig(val uuid: String, val pin: String, val leds: Int, val brightness: Int)