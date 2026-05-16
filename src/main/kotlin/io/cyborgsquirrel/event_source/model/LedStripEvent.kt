package io.cyborgsquirrel.event_source.model

sealed class LedStripEvent(uuid: String) : SseEvent(uuid) {
    class LedStripCreated(uuid: String) : LedStripEvent(uuid)
    class LedStripUpdated(uuid: String) : LedStripEvent(uuid)
    class LedStripDeleted(uuid: String) : LedStripEvent(uuid)
}
