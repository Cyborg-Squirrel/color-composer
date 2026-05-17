package io.cyborgsquirrel.lighting.effects.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class UpdateEffectSettingsRequest(
    val name: String?,
    val settings: Map<String, Any>?,
    val isDefault: Boolean?,
)
