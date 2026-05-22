package io.cyborgsquirrel.lighting.enums

enum class LightEffectStatus {
    // Inactive effect
    Inactive,

    // Currently being rendered
    Playing,

    // No longer being rendered, will resume where it left off if the user reactivates it
    Paused,

    // No longer being rendered, will start from the beginning if the user reactivates it
    Stopped;

    companion object {
        fun inUseStatuses(): List<LightEffectStatus> {
            return listOf(Playing, Paused)
        }
    }
}

fun LightEffectStatus.isInUse(): Boolean {
    return LightEffectStatus.inUseStatuses().contains(this)
}

