package io.cyborgsquirrel.clients.requests

import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class CreateClientRequest(
    val name: String,
    val address: String,
    val clientType: ClientType,
    val colorOrder: ColorOrder?,
    val apiPort: Int,
    val wsPort: Int,
    val powerLimit: Int?
)