package io.cyborgsquirrel.lighting.filters.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class BrightnessFilterSettings(val brightness: Float = 1.0f)