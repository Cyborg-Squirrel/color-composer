package io.cyborgsquirrel.lighting.effects.requests

import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class UpdateEffectRequest(
    val stripUuid: String?,
    val name: String?,
    val effectType: String?,
    val settings: Map<String, Any>?,
    val status: LightEffectStatus?
)