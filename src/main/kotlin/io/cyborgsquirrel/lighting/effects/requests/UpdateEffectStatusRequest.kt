package io.cyborgsquirrel.lighting.effects.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class UpdateEffectStatusRequest(
    val uuids: List<String>,
    val command: LightEffectStatusCommand
)