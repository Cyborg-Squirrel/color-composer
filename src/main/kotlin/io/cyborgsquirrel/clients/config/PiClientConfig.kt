package io.cyborgsquirrel.clients.config

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class PiClientConfig(val uuid: String, val pin: String, val leds: Int, val brightness: Int)