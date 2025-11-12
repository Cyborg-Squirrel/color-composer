package io.cyborgsquirrel.lighting.enums

enum class LightEffectStatus {
    // New effects, or effects which are not stopped or paused but are inactive
    Idle,

    // Currently being rendered
    Playing,

    // No longer being rendered, will resume where it left off if the user reactivates it
    Paused,

    // No longer being rendered, will start from the beginning if the user reactivates it
    Stopped,
}

fun LightEffectStatus.isActive(): Boolean {
    return this == LightEffectStatus.Playing || this == LightEffectStatus.Paused
}