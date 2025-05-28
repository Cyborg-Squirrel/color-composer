package io.cyborgsquirrel.lighting.enums

enum class LightEffectStatus {
    // Newly created, has not yet been rendered
    Created,

    // User or trigger requested the effect to start, hasn't been sent to a led strip client yet
    Activated,

    // Currently being rendered
    Playing,

    // No longer being rendered, will resume where it left off if the user reactivates it
    Paused,

    // The user or a trigger requested this effect stop, it is not yet stopped
    Stopping,

    // No longer being rendered, will start from the beginning if the user reactivates it
    Stopped,
}