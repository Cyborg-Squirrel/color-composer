package io.cyborgsquirrel.lighting.effects.schemas

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class EffectSettingsSchema(val effectName: String, val fields: List<EffectSettingsSchemaField>) {

    fun validate() {
        for (field in fields) field.validate()
    }
}
