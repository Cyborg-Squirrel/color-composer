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
        if (validatorTypes.size != validatorTypes.toSet().size) throw Exception("Duplicate validators for $key!")

        val min = validators.filterIsInstance<EffectSettingsValidator.Min>().firstOrNull()
        val max = validators.filterIsInstance<EffectSettingsValidator.Max>().firstOrNull()
        if (min != null && max != null && min.value >= max.value) throw Exception("$key has a min ${min.value} less than or equal to max ${max.value}!")
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

        fun color(key: String, validators: List<EffectSettingsValidator>, description: String) =
            EffectSettingsSchemaField(key, EffectSettingsType.RgbColor, validators, description)
    }
}
