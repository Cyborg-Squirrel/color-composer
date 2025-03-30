package io.cyborgsquirrel.clients.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class CreateClientRequest(val name: String, val address: String, val apiPort: Int, val wsPort: Int)