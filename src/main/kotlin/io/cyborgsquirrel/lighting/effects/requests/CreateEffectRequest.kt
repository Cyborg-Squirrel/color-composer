package io.cyborgsquirrel.lighting.effects.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class CreateEffectRequest(
    val stripUuid: String?,
    val poolUuid: String?,
    val effectType: String,
    val name: String,
    val settings: Map<String, Any>,
    val paletteUuid: String?
)