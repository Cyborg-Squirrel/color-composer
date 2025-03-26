package io.cyborgsquirrel.lighting.rendering.filters.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class BrightnessFilterSettings(val brightness: Float = 1.0f)