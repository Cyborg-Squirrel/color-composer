package io.cyborgsquirrel.event_source.model

import io.cyborgsquirrel.event_source.model.delta.EffectSettingsDelta
import io.cyborgsquirrel.lighting.effects.responses.GetEffectSettingsResponse

sealed class EffectSettingsEvent(uuid: String, data: Any?) : SseEvent(uuid, data) {
    class EffectSettingsCreated(uuid: String, settings: GetEffectSettingsResponse) : EffectSettingsEvent(uuid, settings)
    class EffectSettingsUpdated(uuid: String, delta: EffectSettingsDelta) : EffectSettingsEvent(uuid, delta)
    class EffectSettingsDeleted(uuid: String) : EffectSettingsEvent(uuid, null)
}
