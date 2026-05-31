package io.cyborgsquirrel.event_source.model

import io.cyborgsquirrel.clients.responses.GetClientResponse
import io.cyborgsquirrel.event_source.model.delta.ClientDelta

sealed class LedClientEvent(uuid: String, data: Any?) : SseEvent(uuid, data) {
    class LedClientCreated(uuid: String, client: GetClientResponse) : LedClientEvent(uuid, client)
    class LedClientUpdated(uuid: String, delta: ClientDelta) : LedClientEvent(uuid, delta)
    class LedClientDeleted(uuid: String) : LedClientEvent(uuid, null)
}
