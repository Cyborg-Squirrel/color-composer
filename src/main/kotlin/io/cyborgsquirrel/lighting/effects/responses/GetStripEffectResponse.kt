package io.cyborgsquirrel.lighting.effects.responses

import io.cyborgsquirrel.lighting.enums.EffectCategory
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetStripEffectResponse(
    override val name: String,
    override val type: String,
    override val uuid: String,
    val stripUuid: String,
    override val paletteUuid: String?,
    override val settingsUuid: String?,
    override val status: LightEffectStatus,
    override val category: EffectCategory,
) : GetEffectResponse(
    name, type, uuid, paletteUuid, settingsUuid, status, category
)