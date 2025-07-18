package io.cyborgsquirrel.lighting.effects.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class UpdateEffectRequest(
    val stripUuid: String?,
    val paletteUuid: String?,
    val name: String?,
    val settings: Map<String, Any>?
)