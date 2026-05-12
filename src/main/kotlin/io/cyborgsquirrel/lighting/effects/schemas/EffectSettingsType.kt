package io.cyborgsquirrel.lighting.effects.schemas

import io.micronaut.serde.annotation.Serdeable

@Serdeable
enum class EffectSettingsType {
    Boolean,
    Integer,
    Number,
    String,
    RgbColor,
}