package io.cyborgsquirrel.event_source.model

import io.cyborgsquirrel.event_source.model.delta.PoolDelta
import io.cyborgsquirrel.strip_pools.responses.GetStripPoolResponse

sealed class StripPoolEvent(uuid: String, data: Any?) : SseEvent(uuid, data) {
    class StripPoolCreated(uuid: String, pool: GetStripPoolResponse) : StripPoolEvent(uuid, pool)
    class StripPoolUpdated(uuid: String, delta: PoolDelta) : StripPoolEvent(uuid, delta)
    class StripPoolDeleted(uuid: String) : StripPoolEvent(uuid, null)
}
