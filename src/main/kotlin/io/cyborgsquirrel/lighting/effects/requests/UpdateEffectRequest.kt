package io.cyborgsquirrel.lighting.effects.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class UpdateEffectRequest(
    val stripUuid: String?,
    val poolUuid: String?,
    val paletteUuid: String?,
    val settingsUuid: String?,
    val name: String?,
)