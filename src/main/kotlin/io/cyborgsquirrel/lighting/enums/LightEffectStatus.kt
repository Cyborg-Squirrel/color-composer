package io.cyborgsquirrel.lighting.enums

enum class LightEffectStatus {
    // Actively being rendered
    Active,

    // No longer being rendered, will resume where it left off if the user reactivates it
    Paused,

    // No longer being rendered, will start from the beginning if the user reactivates it
    Stopped,
}