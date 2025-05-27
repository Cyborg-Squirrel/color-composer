package io.cyborgsquirrel.lighting.effects.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class CreateActiveEffectRequest(
    val stripUuid: String,
    val effectUuid: String
)