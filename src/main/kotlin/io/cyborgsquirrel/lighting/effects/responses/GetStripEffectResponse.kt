package io.cyborgsquirrel.lighting.effects.responses

import io.cyborgsquirrel.lighting.enums.LightEffectStatus

data class GetStripEffectResponse(
    override val name: String,
    override val type: String,
    override val uuid: String,
    val stripUuid: String,
    override val paletteUuid: String?,
    override val settings: Map<String, Any>,
    override val status: LightEffectStatus,
) : GetEffectResponse(
    name, type, uuid, paletteUuid, settings, status
)