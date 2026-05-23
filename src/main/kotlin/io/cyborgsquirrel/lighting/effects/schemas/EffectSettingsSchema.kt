package io.cyborgsquirrel.lighting.effects.schemas

import io.cyborgsquirrel.lighting.enums.EffectCategory
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class EffectSettingsSchema(
    val effectName: String,
    val category: EffectCategory,
    val fields: List<EffectSettingsSchemaField>
) {

    fun validate() {
        for (field in fields) field.validate()
    }
}
