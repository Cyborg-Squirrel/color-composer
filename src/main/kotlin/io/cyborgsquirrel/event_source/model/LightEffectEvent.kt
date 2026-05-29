package io.cyborgsquirrel.event_source.model

import io.cyborgsquirrel.event_source.model.delta.EffectDelta
import io.cyborgsquirrel.lighting.effects.responses.GetEffectResponse

sealed class LightEffectEvent(uuid: String, data: Any?) : SseEvent(uuid, data) {
    class LightEffectCreated(uuid: String, effect: GetEffectResponse) : LightEffectEvent(uuid, effect)
    class LightEffectUpdated(uuid: String, delta: EffectDelta) : LightEffectEvent(uuid, delta)
    class LightEffectDeleted(uuid: String) : LightEffectEvent(uuid, null)
}
