package io.cyborgsquirrel.lighting.rendering.filters

import io.cyborgsquirrel.lighting.rendering.filters.settings.BrightnessFilterSettings
import io.cyborgsquirrel.model.color.RgbColor
import io.micronaut.serde.annotation.Serdeable

/**
 * Transforms a list of [RgbColor] to increase or decrease the brightness
 */
@Serdeable
open class BrightnessFilter(val settings: BrightnessFilterSettings, uuid: String) : LightEffectFilter(uuid) {

    private val brightness = settings.brightness

    /**
     * Scales the list of [RgbColor] by the [brightness] value.
     * A [brightness] of 0.0f will set them to blank, [brightness] over 0 and less than 1 will reduce the brightness,
     * greater than 1 will increase the brightness.
     */
    override fun apply(rgbList: List<RgbColor>): List<RgbColor> {
        return rgbList.map {
            it.scale(settings.brightness)
        }
    }
}