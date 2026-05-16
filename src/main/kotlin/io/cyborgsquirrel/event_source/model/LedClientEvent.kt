package io.cyborgsquirrel.event_source.model

sealed class LedClientEvent(uuid: String) : SseEvent(uuid) {
    class LedClientCreated(uuid: String) : LedClientEvent(uuid)
    class LedClientUpdated(uuid: String) : LedClientEvent(uuid)
    class LedClientDeleted(uuid: String) : LedClientEvent(uuid)
}
