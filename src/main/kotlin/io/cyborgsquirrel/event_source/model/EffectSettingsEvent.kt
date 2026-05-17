package io.cyborgsquirrel.event_source.model

sealed class EffectSettingsEvent(uuid: String) : SseEvent(uuid) {
    class EffectSettingsCreated(uuid: String) : EffectSettingsEvent(uuid)
    class EffectSettingsUpdated(uuid: String) : EffectSettingsEvent(uuid)
    class EffectSettingsDeleted(uuid: String) : EffectSettingsEvent(uuid)
}
