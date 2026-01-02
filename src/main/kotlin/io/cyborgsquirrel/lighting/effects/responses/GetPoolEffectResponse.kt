package io.cyborgsquirrel.lighting.effects.responses

import io.cyborgsquirrel.lighting.enums.LightEffectStatus

data class GetPoolEffectResponse(
    override val name: String,
    override val type: String,
    override val uuid: String,
    val poolUuid: String,
    override val paletteUuid: String?,
    override val settings: Map<String, Any>,
    override val status: LightEffectStatus,
) : GetEffectResponse(
    name, type, uuid, paletteUuid, settings, status
)