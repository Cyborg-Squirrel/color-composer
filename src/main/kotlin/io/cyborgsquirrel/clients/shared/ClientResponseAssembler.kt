package io.cyborgsquirrel.clients.shared

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.responses.GetClientResponse
import io.cyborgsquirrel.clients.status.ClientStatusInfo
import io.cyborgsquirrel.clients.status.ClientStatusService
import jakarta.inject.Singleton

/**
 * Maps a [LedStripClientEntity] to the `GET`-shaped [GetClientResponse], enriching it with runtime
 * status from [ClientStatusService].
 *
 * Shared because both the query side (read endpoints) and the create command need this mapping — the
 * `LedClientCreated` event payload carries the full response shape. Extracting it here keeps the
 * command from depending on a query handler and avoids duplicating the status-enrichment logic.
 */
@Singleton
class ClientResponseAssembler(
    private val clientStatusService: ClientStatusService,
) {
    fun mapClientEntityToResponse(client: LedStripClientEntity): GetClientResponse {
        val statusInfo = getStatusInfo(client)
        return GetClientResponse(
            client.name,
            client.address,
            client.uuid,
            client.clientType.toString(),
            client.colorOrder,
            client.apiPort,
            client.wsPort,
            client.lastSeenAt,
            statusInfo.status,
            statusInfo.activeEffects,
            client.powerLimit,
            client.firmwareVersion,
            client.fps,
            client.fadeTimeoutMillis,
        )
    }

    private fun getStatusInfo(client: LedStripClientEntity): ClientStatusInfo {
        val statusOptional = clientStatusService.getStatusForClient(client)
        return if (statusOptional.isPresent) {
            statusOptional.get()
        } else {
            ClientStatusInfo.error()
        }
    }
}
