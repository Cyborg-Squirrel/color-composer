package io.cyborgsquirrel.event_source.model

sealed class LightEffectEvent(uuid: String) : SseEvent(uuid) {
    class LightEffectCreated(uuid: String) : LightEffectEvent(uuid)
    class LightEffectUpdated(uuid: String) : LightEffectEvent(uuid)
    class LightEffectDeleted(uuid: String) : LightEffectEvent(uuid)
}