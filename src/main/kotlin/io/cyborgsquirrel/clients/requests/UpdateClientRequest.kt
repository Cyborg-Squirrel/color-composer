package io.cyborgsquirrel.clients.requests

import io.cyborgsquirrel.clients.enums.ColorOrder
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class UpdateClientRequest(
    val name: String?,
    val address: String?,
    val colorOrder: ColorOrder?,
    val apiPort: Int?,
    val wsPort: Int?,
    val powerLimit: Int?
)