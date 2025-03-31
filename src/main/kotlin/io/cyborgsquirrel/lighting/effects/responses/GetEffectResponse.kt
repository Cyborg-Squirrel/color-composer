package io.cyborgsquirrel.lighting.effects.responses

import io.cyborgsquirrel.lighting.enums.LightEffectStatus

data class GetEffectResponse(
    val name: String,
    val uuid: String,
    val stripUuid: String,
    val settings: Map<String, Any>,
    val status: LightEffectStatus,
)
