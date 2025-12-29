package io.cyborgsquirrel.clients.responses

import io.cyborgsquirrel.clients.enums.ClientStatus
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetClientResponse(
    val name: String,
    val address: String,
    val uuid: String,
    val clientType: String,
    val colorOrder: ColorOrder,
    val apiPort: Int,
    val wsPort: Int,
    val lastSeenAt: Long,
    val status: ClientStatus,
    val activeEffects: Int,
    val powerLimit: Int?
)