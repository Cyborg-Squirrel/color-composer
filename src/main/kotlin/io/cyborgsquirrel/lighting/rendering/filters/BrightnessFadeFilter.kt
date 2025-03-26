package io.cyborgsquirrel.lighting.rendering.filters

import io.cyborgsquirrel.lighting.rendering.filters.settings.BrightnessFadeFilterSettings
import io.cyborgsquirrel.model.color.RgbColor
import io.cyborgsquirrel.util.time.TimeHelper
import io.micronaut.serde.annotation.Serdeable
import java.time.Duration

/**
 * Transforms a list of [RgbColor] by a specified fade-in/fade-out brightness.
 * If [startingBrightness] is lower than [endingBrightness] the effect will fade-in, a higher value will fade-out.
 * A timestamp is recorded the first time this filter is used and will return the [endingBrightness] after the amount
 * of time specified in [fadeDuration] has passed.
 */
@Serdeable
open class BrightnessFadeFilter(
    val settings: BrightnessFadeFilterSettings,
    val timeHelper: TimeHelper,
    uuid: String,
) : LightEffectFilter(uuid) {

    private var startTimeEpochMillis = 0L
    private val startingBrightness = settings.startingBrightness
    private val endingBrightness = settings.endingBrightness
    private val fadeDuration = settings.fadeDuration

    /**
     * Scales the list of [RgbColor] from [startingBrightness] to the [endingBrightness] value.
     */
    override fun apply(rgbList: List<RgbColor>): List<RgbColor> {
        val millisSinceEpoch = timeHelper.millisSinceEpoch()
        return if (startTimeEpochMillis == 0L) {
            startTimeEpochMillis = millisSinceEpoch
            return rgbList.map {
                it.scale(startingBrightness)
            }
        } else {
            if (startTimeEpochMillis + settings.fadeDuration.toMillis() < millisSinceEpoch) {
                rgbList.map {
                    it.scale(endingBrightness)
                }
            } else {
                val millisSinceStart = millisSinceEpoch - startTimeEpochMillis
                val percentComplete = millisSinceStart.toFloat() / fadeDuration.toMillis()
                val startingBrightnessList = rgbList.map {
                    it.scale(startingBrightness)
                }
                val endingBrightnessList = rgbList.map {
                    it.scale(endingBrightness)
                }
                val currentBrightnessList = startingBrightnessList.mapIndexed { index, rgbColor ->
                    rgbColor.interpolate(endingBrightnessList[index], percentComplete)
                }

                currentBrightnessList
            }
        }
    }
}