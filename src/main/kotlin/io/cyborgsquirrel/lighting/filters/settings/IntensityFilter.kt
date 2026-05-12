package io.cyborgsquirrel.lighting.filters.settings

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class IntensityFilter(val intensity: Float = 1.0f) : LightEffectFilter()