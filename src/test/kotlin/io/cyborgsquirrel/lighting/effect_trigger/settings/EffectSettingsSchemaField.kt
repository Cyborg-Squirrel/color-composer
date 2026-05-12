package io.cyborgsquirrel.lighting.effect_trigger.settings

import io.cyborgsquirrel.lighting.effects.schemas.EffectSettingsType
import io.cyborgsquirrel.lighting.effects.schemas.EffectSettingsValidator

data class EffectSettingsSchemaField(
    val key: String,
    val type: EffectSettingsType,
    val description: String = "",
    val validators: List<EffectSettingsValidator>
)