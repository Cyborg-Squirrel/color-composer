package io.cyborgsquirrel.lighting.rendering.filters

import io.cyborgsquirrel.model.color.RgbColor
import io.cyborgsquirrel.util.time.TimeHelper
import java.time.Duration

/**
 * Transforms a list of [RgbColor] by a specified fade-in/fade-out brightness.
 * If [startingBrightness] is lower than [endingBrightness] the effect will fade-in, a higher value will fade-out.
 * A timestamp is recorded the first time this filter is used and will return the [endingBrightness] after the amount
 * of time specified in [fadeDuration] has passed.
 */
class BrightnessFadeFilter(
    val startingBrightness: Float,
    val endingBrightness: Float,
    val fadeDuration: Duration,
    val timeHelper: TimeHelper
) :
    BrightnessFilter(endingBrightness) {

    private var startTimeEpochMillis = 0L

    /**
     * Scales the list of [RgbColor] from [startingBrightness] to the [endingBrightness] value.
     */
    override fun apply(rgbList: List<RgbColor>): List<RgbColor> {
        val millisSinceEpoch = timeHelper.millisSinceEpoch()
        if (startTimeEpochMillis == 0L) {
            startTimeEpochMillis = millisSinceEpoch
            return rgbList.map {
                it.scale(startingBrightness)
            }
        } else {
            return if (startTimeEpochMillis + fadeDuration.toMillis() < millisSinceEpoch) {
                super.apply(rgbList)
            } else {
                val millisSinceStart = millisSinceEpoch - startTimeEpochMillis
                val percentComplete = millisSinceStart.toFloat() / fadeDuration.toMillis()
                val startingBrightnessList = rgbList.map {
                    it.scale(startingBrightness)
                }
                val endingBrightnessList = rgbList.map {
                    it.scale(endingBrightness)
                }
                val currentBrightnessList = mutableListOf<RgbColor>()
                for (i in startingBrightnessList.indices) {
                    val interpolatedBrightnessRgb =
                        startingBrightnessList[i].interpolate(endingBrightnessList[i], percentComplete)
                    currentBrightnessList.add(interpolatedBrightnessRgb)
                }

                currentBrightnessList
            }
        }
    }
}