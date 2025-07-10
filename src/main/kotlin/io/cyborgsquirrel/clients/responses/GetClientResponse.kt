package io.cyborgsquirrel.clients.responses

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetClientResponse(
    val name: String,
    val address: String,
    val uuid: String,
    val clientType: String,
    val colorOrder: String,
    val apiPort: Int,
    val wsPort: Int
)