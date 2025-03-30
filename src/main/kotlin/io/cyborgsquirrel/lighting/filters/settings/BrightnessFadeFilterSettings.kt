package io.cyborgsquirrel.lighting.filters.settings

import io.micronaut.serde.annotation.Serdeable
import java.time.Duration

@Serdeable
data class BrightnessFadeFilterSettings(
    val startingBrightness: Float,
    val endingBrightness: Float,
    val fadeDuration: Duration,
)