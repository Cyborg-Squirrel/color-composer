package io.cyborgsquirrel.strip_pools.responses

import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetStripPoolResponse(
    val name: String,
    val uuid: String,
    val poolType: PoolType,
    val blendMode: BlendMode,
    val members: List<StripPoolMemberResponseModel>
)