package io.cyborgsquirrel.lighting.filters

import io.cyborgsquirrel.lighting.filters.settings.IntensityFilterSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import io.micronaut.serde.annotation.Serdeable

/**
 * Transforms a list of [RgbColor] to increase or decrease the intensity
 */
@Serdeable
open class IntensityFilter(val settings: IntensityFilterSettings, uuid: String) : LightEffectFilter(uuid) {

    private val intensity = settings.intensity

    /**
     * Scales the list of [RgbColor] by the [intensity] value.
     * A [intensity] of 0.0f will set them to blank, [intensity] over 0 and less than 1 will reduce the intensity,
     * greater than 1 will increase the intensity.
     */
    override fun apply(rgbList: List<RgbColor>): List<RgbColor> {
        return rgbList.map {
            it.scale(intensity)
        }
    }
}