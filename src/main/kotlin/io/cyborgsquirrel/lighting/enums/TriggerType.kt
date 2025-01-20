package io.cyborgsquirrel.lighting.enums

enum class TriggerType {
    // Indicates the effect should turn on when the trigger activates
    Activate,

    // Indicates the effect should turn off when the trigger activates
    Deactivate,

    // Similar to [Deactivate] but indicates the effect should wind down slowly
    FadeOut
}