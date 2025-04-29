package io.cyborgsquirrel.lighting.effects.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class UpdateEffectRequest(
    val stripUuid: String?,
    val name: String?,
    val settings: Map<String, Any>?
)