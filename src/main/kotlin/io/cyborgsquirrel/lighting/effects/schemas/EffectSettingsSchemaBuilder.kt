package io.cyborgsquirrel.lighting.effects.schemas

class EffectSettingsSchemaBuilder(private val effectName: String) {
    private val fields = mutableListOf<EffectSettingsSchemaField>()
    private var built = false

    class FieldBuilder {
        internal val validators = mutableListOf<EffectSettingsValidator>()

        fun min(value: Double) { validators.add(EffectSettingsValidator.Min(value)) }
        fun max(value: Double) { validators.add(EffectSettingsValidator.Max(value)) }
        fun options(values: List<String>) { validators.add(EffectSettingsValidator.Options(values)) }
    }

    fun integer(key: String, description: String = "", block: FieldBuilder.() -> Unit = {}) =
        addField(key, EffectSettingsType.Integer, description, block)

    fun number(key: String, description: String = "", block: FieldBuilder.() -> Unit = {}) =
        addField(key, EffectSettingsType.Number, description, block)

    fun string(key: String, description: String = "", block: FieldBuilder.() -> Unit = {}) =
        addField(key, EffectSettingsType.String, description, block)

    fun boolean(key: String, description: String = "", block: FieldBuilder.() -> Unit = {}) =
        addField(key, EffectSettingsType.Boolean, description, block)

    private fun addField(
        key: String,
        type: EffectSettingsType,
        description: String,
        block: FieldBuilder.() -> Unit,
    ): EffectSettingsSchemaBuilder {
        val fb = FieldBuilder().apply(block)
        fields.add(EffectSettingsSchemaField(key, type, fb.validators.toList(), description))
        return this
    }

    fun build(): EffectSettingsSchema {
        check(!built) { "build() has already been called" }
        built = true
        fields.forEach { it.validate() }
        return EffectSettingsSchema(effectName, fields)
    }
}
