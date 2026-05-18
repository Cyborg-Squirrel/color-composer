package io.cyborgsquirrel.lighting.effects.responses

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetEffectSettingsResponse(
    val uuid: String,
    val type: String,
    val name: String,
    val settings: Map<String, Any>,
    val isDefault: Boolean,
)
