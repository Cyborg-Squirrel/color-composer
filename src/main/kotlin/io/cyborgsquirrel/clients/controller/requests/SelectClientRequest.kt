package io.cyborgsquirrel.clients.controller.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class SelectClientRequest(val name: String, val address: String)