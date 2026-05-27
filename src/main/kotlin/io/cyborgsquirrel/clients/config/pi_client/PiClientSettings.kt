package io.cyborgsquirrel.clients.config.pi_client

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.serde.annotation.Serdeable

@Serdeable
class PiClientSettings (
    @param:JsonProperty("power_limit")
    val powerLimit: Int,

    @param:JsonProperty("fade_timeout_millis")
    val fadeTimeoutMillis: Int
)