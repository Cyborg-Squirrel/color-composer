package io.cyborgsquirrel.event_source.model.delta

import com.fasterxml.jackson.annotation.JsonInclude
import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.strip_pools.responses.StripPoolMemberResponseModel
import io.micronaut.serde.annotation.Serdeable

/**
 * Delta payload for `StripPoolUpdated` events. Only the fields that changed are populated; the rest
 * are null and omitted from the serialized JSON. When the member list is replaced, [members] holds
 * the full new member list.
 */
@Serdeable.Serializable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PoolDelta(
    val name: String? = null,
    val poolType: PoolType? = null,
    val blendMode: BlendMode? = null,
    val members: List<StripPoolMemberResponseModel>? = null,
)
