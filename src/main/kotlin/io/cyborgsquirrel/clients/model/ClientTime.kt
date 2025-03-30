package io.cyborgsquirrel.clients.model

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ClientTime(val millisSinceEpoch: Long)