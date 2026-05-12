package io.cyborgsquirrel.lighting.effects.schemas

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(name = "min", value = EffectSettingsValidator.Min::class),
    JsonSubTypes.Type(name = "max", value = EffectSettingsValidator.Max::class),
    JsonSubTypes.Type(name = "options", value = EffectSettingsValidator.Options::class),
)
sealed class EffectSettingsValidator {

    @Serdeable
    data class Min(val value: Double) : EffectSettingsValidator()

    @Serdeable
    data class Max(val value: Double) : EffectSettingsValidator()

    @Serdeable
    data class Options(val values: List<String>) : EffectSettingsValidator()
}
