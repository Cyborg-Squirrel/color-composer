package io.cyborgsquirrel.lighting.effects.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
enum class LightEffectStatusCommand {
    Deactivate,
    Play,
    Pause,
    Stop
}