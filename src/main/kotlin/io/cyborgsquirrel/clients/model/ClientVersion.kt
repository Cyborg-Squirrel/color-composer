package io.cyborgsquirrel.clients.model

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ClientVersion(val version: String)
