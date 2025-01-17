package io.cyborgsquirrel.lighting.rendering.filters

import io.cyborgsquirrel.model.color.RgbColor

/**
 * Transforms a list of [RgbColor] to increase or decrease the brightness
 */
class BrightnessFilter(var brightness: Float = 1.0f) : LightEffectFilter {

    /**
     * Scales the list of [RgbColor] by the [brightness] value.
     * A [brightness] of 0.0f will set them to blank, [brightness] over 0 and less than 1 will reduce the brightness,
     * greater than 1 will increase the brightness.
     */
    override fun apply(rgbList: List<RgbColor>): List<RgbColor> {
        return rgbList.map {
            it.scale(brightness)
        }
    }
}