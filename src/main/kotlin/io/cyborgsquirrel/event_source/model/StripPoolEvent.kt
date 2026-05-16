package io.cyborgsquirrel.event_source.model

sealed class StripPoolEvent(uuid: String) : SseEvent(uuid) {
    class StripPoolCreated(uuid: String) : StripPoolEvent(uuid)
    class StripPoolUpdated(uuid: String) : StripPoolEvent(uuid)
    class StripPoolDeleted(uuid: String) : StripPoolEvent(uuid)
}
