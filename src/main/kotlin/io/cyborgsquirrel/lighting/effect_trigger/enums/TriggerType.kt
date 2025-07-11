package io.cyborgsquirrel.lighting.effect_trigger.enums

import io.micronaut.serde.annotation.Serdeable

@Serdeable
enum class TriggerType {
    // Indicates the effect should start when the trigger activates, until the configured end of the trigger
    StartEffect,

    // Indicates the effect should stop when the trigger activates
    StopEffect,
}