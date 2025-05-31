package io.cyborgsquirrel.lighting.effects.requests

import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class UpdateEffectStatusRequest(
    val uuids: List<String>,
    val status: LightEffectStatus
)