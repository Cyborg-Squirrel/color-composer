package io.cyborgsquirrel.lighting.effects.responses

import io.cyborgsquirrel.lighting.enums.EffectCategory
import io.cyborgsquirrel.lighting.enums.LightEffectStatus

sealed class GetEffectResponse(
    open val name: String,
    open val type: String,
    open val uuid: String,
    open val paletteUuid: String?,
    open val settingsUuid: String?,
    open val status: LightEffectStatus,
    open val category: EffectCategory,
)
