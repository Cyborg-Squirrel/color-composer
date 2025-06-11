package io.cyborgsquirrel.lighting.filters.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class CreateEffectFilterRequest(
    val name: String,
    val filterType: String,
    val settings: Map<String, Any>
)