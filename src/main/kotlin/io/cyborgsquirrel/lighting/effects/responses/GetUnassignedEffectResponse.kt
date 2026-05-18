package io.cyborgsquirrel.lighting.effects.responses

import io.cyborgsquirrel.lighting.enums.EffectCategory
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.micronaut.serde.annotation.Serdeable

/**
 * API response object for effects which are not assigned to a strip or strip pool
 */
@Serdeable
data class GetUnassignedEffectResponse(
    override val name: String,
    override val type: String,
    override val uuid: String,
    override val paletteUuid: String?,
    override val settingsUuid: String?,
    override val status: LightEffectStatus,
    override val category: EffectCategory,
) : GetEffectResponse(
    name, type, uuid, paletteUuid, settingsUuid, status, category
)