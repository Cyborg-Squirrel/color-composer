package io.cyborgsquirrel.lighting.effects.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class UpdateEffectRequest(
    val unassignPalette: Boolean = false,
    val paletteUuid: String?,
    val settingsUuid: String?,
    val name: String?,
    val layer: Int? = null,
)