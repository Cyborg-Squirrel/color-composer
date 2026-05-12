package io.cyborgsquirrel.lighting.filters.settings

import io.micronaut.serde.annotation.Serdeable
import java.time.Duration

@Serdeable
data class IntensityFadeFilter(
    val startingIntensity: Float,
    val endingIntensity: Float,
    val fadeDuration: Duration,
) : LightEffectFilter()