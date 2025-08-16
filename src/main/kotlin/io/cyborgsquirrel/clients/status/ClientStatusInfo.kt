package io.cyborgsquirrel.clients.status

import io.cyborgsquirrel.clients.enums.ClientStatus

data class ClientStatusInfo(
    val status: ClientStatus,
    val activeEffects: Int,
) {
    companion object {
        fun inactive(status: ClientStatus) = ClientStatusInfo(status, 0)
        fun error() = ClientStatusInfo(ClientStatus.Error, 0)
    }
}
