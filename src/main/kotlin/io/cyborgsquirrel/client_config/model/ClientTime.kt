package io.cyborgsquirrel.client_config.model

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ClientTime(val millisSinceEpoch: Long)