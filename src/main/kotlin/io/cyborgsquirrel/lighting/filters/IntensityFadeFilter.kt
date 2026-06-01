package io.cyborgsquirrel.lighting.filters

import io.cyborgsquirrel.lighting.filters.settings.IntensityFadeFilterSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.util.time.TimeHelper
import io.micronaut.serde.annotation.Serdeable

/**
 * Transforms a list of [RgbColor] by a specified fade-in/fade-out intensity.
 * If [startingIntensity] is lower than [endingIntensity] the effect will fade-in, a higher value will fade-out.
 * A timestamp is recorded the first time this filter is used and will return the [endingIntensity] after the amount
 * of time specified in [fadeDuration] has passed.
 */
@Serdeable
open class IntensityFadeFilter(
    val settings: IntensityFadeFilterSettings,
    val timeHelper: TimeHelper,
    uuid: String,
) : LightEffectFilter(uuid) {

    private var startTimeEpochMillis = 0L
    private val startingIntensity = settings.startingIntensity
    private val endingIntensity = settings.endingIntensity
    private val fadeDuration = settings.fadeDuration

    /**
     * Scales the list of [RgbColor] from [startingIntensity] to the [endingIntensity] value.
     */
    override fun apply(rgbList: List<RgbColor>): List<RgbColor> {
        // Interpolating per channel and then blending the two scaled buffers is equivalent to scaling once by the
        // interpolated intensity, so compute a single factor and apply it in one pass.
        val intensity = currentIntensity()
        return rgbList.map { it.scale(intensity) }
    }

    private fun currentIntensity(): Float {
        val millisSinceEpoch = timeHelper.millisSinceEpoch()
        val fadeDurationMillis = fadeDuration.toMillis()
        if (startTimeEpochMillis == 0L) {
            startTimeEpochMillis = millisSinceEpoch
            return if (fadeDurationMillis == 0L) endingIntensity else startingIntensity
        }
        if (startTimeEpochMillis + fadeDurationMillis < millisSinceEpoch) {
            return endingIntensity
        }
        val percentComplete = (millisSinceEpoch - startTimeEpochMillis).toFloat() / fadeDurationMillis
        return startingIntensity + (endingIntensity - startingIntensity) * percentComplete
    }
}