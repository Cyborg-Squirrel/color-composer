package io.cyborgsquirrel.lighting.filters.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class IntensityFilterSettings(val intensity: Float = 1.0f)