package io.cyborgsquirrel.lighting.effect_palette.helper

import io.cyborgsquirrel.lighting.effect_palette.settings.Palette
import io.cyborgsquirrel.lighting.model.LedStrip

class GradientColorHelper {

    fun getPalette(index: Int, strip: LedStrip, points: Map<Int, Palette>): Palette {
        var gradientStartKey = points.keys.first()
        var gradientEndKey = points.keys.first()
        for (point in points.keys) {
            if (index / strip.getLength() <= gradientStartKey) {
                gradientStartKey = point
            } else if (index / strip.getLength() >= point) {
                gradientEndKey = point
            }
        }

        val gradientLength = gradientEndKey - gradientStartKey
        val gradientPercent = 1 - ((gradientEndKey - index) / gradientLength).toFloat()
        return points[gradientStartKey]!!.interpolate(points[gradientEndKey]!!, gradientPercent)
    }
}