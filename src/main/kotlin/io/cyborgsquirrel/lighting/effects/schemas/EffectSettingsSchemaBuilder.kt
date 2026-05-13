package io.cyborgsquirrel.lighting.effects.schemas

class EffectSettingsSchemaBuilder(private val effectName: String) {
    private val completedFields = mutableListOf<EffectSettingsSchemaField>()
    private var fieldKey: String? = null
    private var fieldDescription: String? = null
    private var fieldType: EffectSettingsType? = null
    private val validators = mutableListOf<EffectSettingsValidator>()

    fun integer(key: String, description: String) = fieldBuilder(key, EffectSettingsType.Integer, description)
    fun number(key: String, description: String) = fieldBuilder(key, EffectSettingsType.Number, description)
    fun string(key: String, description: String) = fieldBuilder(key, EffectSettingsType.String, description)
    fun boolean(key: String, description: String) = fieldBuilder(key, EffectSettingsType.Boolean, description)

    fun min(value: Double): EffectSettingsSchemaBuilder {
        validators.add(EffectSettingsValidator.Min(value))
        return this
    }

    fun max(value: Double): EffectSettingsSchemaBuilder {
        validators.add(EffectSettingsValidator.Max(value))
        return this
    }

    fun options(values: List<String>): EffectSettingsSchemaBuilder {
        validators.add(EffectSettingsValidator.Options(values))
        return this
    }

    private fun fieldBuilder(
        key: String,
        type: EffectSettingsType,
        description: String
    ): EffectSettingsSchemaBuilder {
        if (fieldKey != null && fieldDescription != null && fieldType != null) {
            completedFields.add(EffectSettingsSchemaField(fieldKey!!, fieldType!!, validators.toList(), fieldDescription!!))
        }

        validators.clear()
        fieldKey = key
        fieldType = type
        fieldDescription = description
        return this
    }

    fun build(): EffectSettingsSchema {
        if (fieldKey != null && fieldDescription != null && fieldType != null) {
            completedFields.add(EffectSettingsSchemaField(fieldKey!!, fieldType!!, validators.toList(), fieldDescription!!))
        }
        completedFields.forEach { it.validate() }
        return EffectSettingsSchema(effectName, completedFields)
    }
}
