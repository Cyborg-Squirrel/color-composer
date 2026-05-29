package io.cyborgsquirrel.event_source.model

import io.cyborgsquirrel.event_source.model.delta.StripDelta
import io.cyborgsquirrel.led_strips.responses.GetLedStripResponse

sealed class LedStripEvent(uuid: String, data: Any?) : SseEvent(uuid, data) {
    class LedStripCreated(uuid: String, strip: GetLedStripResponse) : LedStripEvent(uuid, strip)
    class LedStripUpdated(uuid: String, delta: StripDelta) : LedStripEvent(uuid, delta)
    class LedStripDeleted(uuid: String) : LedStripEvent(uuid, null)
}
