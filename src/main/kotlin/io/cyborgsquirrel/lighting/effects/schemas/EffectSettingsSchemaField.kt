package io.cyborgsquirrel.lighting.effects.schemas

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class EffectSettingsSchemaField(
    val key: String,
    val type: EffectSettingsType,
    val validators: List<EffectSettingsValidator>,
    val description: String,
) {
    fun validate() {
        val validatorTypes = validators.map { it::class }
        if (validatorTypes.size != validatorTypes.toSet().size)
            throw IllegalArgumentException("Duplicate validators for $key!")

        val min = validators.filterIsInstance<EffectSettingsValidator.Min>().firstOrNull()
        val max = validators.filterIsInstance<EffectSettingsValidator.Max>().firstOrNull()
        if (min != null && max != null && min.value >= max.value)
            throw IllegalArgumentException("$key: min (${min.value}) must be less than max (${max.value})")

        if ((min != null || max != null) && type !in setOf(EffectSettingsType.Integer, EffectSettingsType.Number))
            throw IllegalArgumentException("$key: min/max validators are only valid for Integer or Number fields")

        val options = validators.filterIsInstance<EffectSettingsValidator.Options>().firstOrNull()
        if (options != null && type != EffectSettingsType.String)
            throw IllegalArgumentException("$key: options validator is only valid for String fields")
    }

    companion object {
        fun integer(key: String, validators: List<EffectSettingsValidator>, description: String) =
            EffectSettingsSchemaField(key, EffectSettingsType.Integer, validators, description)

        fun number(key: String, validators: List<EffectSettingsValidator>, description: String) =
            EffectSettingsSchemaField(key, EffectSettingsType.Number, validators, description)

        fun string(key: String, validators: List<EffectSettingsValidator>, description: String) =
            EffectSettingsSchemaField(key, EffectSettingsType.String, validators, description)

        fun boolean(key: String, validators: List<EffectSettingsValidator>, description: String) =
            EffectSettingsSchemaField(key, EffectSettingsType.Boolean, validators, description)
    }
}
