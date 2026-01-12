package io.cyborgsquirrel.strip_pools.requests

import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class CreateStripPoolRequest(
    val name: String,
    val poolType: PoolType,
    val blendMode: BlendMode
)