package io.cyborgsquirrel.clients.controller.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class CreateClientRequest(val name: String, val address: String, val apiPort: Int, val wsPort: Int)