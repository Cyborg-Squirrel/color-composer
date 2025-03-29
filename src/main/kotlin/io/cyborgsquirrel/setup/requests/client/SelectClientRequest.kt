package io.cyborgsquirrel.setup.requests.client

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class SelectClientRequest(val name: String, val address: String)