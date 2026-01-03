package io.cyborgsquirrel.lighting.effects.responses

import io.cyborgsquirrel.lighting.enums.LightEffectStatus

sealed class GetEffectResponse(
    open val name: String,
    open val type: String,
    open val uuid: String,
    open val paletteUuid: String?,
    open val settings: Map<String, Any>,
    open val status: LightEffectStatus,
)
