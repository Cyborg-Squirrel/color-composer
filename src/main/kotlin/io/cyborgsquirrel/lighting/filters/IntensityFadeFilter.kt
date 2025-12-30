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
        val millisSinceEpoch = timeHelper.millisSinceEpoch()
        return if (startTimeEpochMillis == 0L) {
            startTimeEpochMillis = millisSinceEpoch
            return rgbList.map {
                it.scale(if (settings.fadeDuration.toMillis() == 0L) endingIntensity else startingIntensity)
            }
        } else {
            if (startTimeEpochMillis + settings.fadeDuration.toMillis() < millisSinceEpoch) {
                rgbList.map {
                    it.scale(endingIntensity)
                }
            } else {
                val millisSinceStart = millisSinceEpoch - startTimeEpochMillis
                val percentComplete = millisSinceStart.toFloat() / fadeDuration.toMillis()
                val startingIntensityList = rgbList.map {
                    it.scale(startingIntensity)
                }
                val endingIntensityList = rgbList.map {
                    it.scale(endingIntensity)
                }
                val currentIntensityList = startingIntensityList.mapIndexed { index, rgbColor ->
                    rgbColor.interpolate(endingIntensityList[index], percentComplete)
                }

                currentIntensityList
            }
        }
    }
}