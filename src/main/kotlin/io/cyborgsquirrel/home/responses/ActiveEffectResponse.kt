package io.cyborgsquirrel.home.responses

import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ActiveEffectResponse(
    val uuid: String,
    val status: LightEffectStatus,
    val stripUuid: String,
)
