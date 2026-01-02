package io.cyborgsquirrel.lighting.effect_palette.helper

import io.cyborgsquirrel.lighting.effect_palette.settings.SettingsPalette

class GradientColorHelper {

    fun getPalette(index: Int, numberOfLeds: Int, points: Map<Int, SettingsPalette>): SettingsPalette {
        var gradientStartPoint = points.keys.first()
        var gradientEndPoint = 0
        val percentDone = (index / numberOfLeds.toFloat()) * 100

        for (point in points.keys) {
            if (percentDone > point) {
                gradientStartPoint = point
            } else if (gradientStartPoint != point) {
                gradientEndPoint = point
                break
            }
        }

        // If the starting section of the gradient is the last point in the gradient then...
        // Set the end point to the end of the gradient and the start point to the second to last point in the gradient
        if (gradientStartPoint == points.keys.max()) {
            gradientEndPoint = gradientStartPoint
            gradientStartPoint = points.keys.sortedBy { it }[points.keys.size - 2]
        }

        val gradientLength = gradientEndPoint - gradientStartPoint
        val gradientPercent = (percentDone - gradientStartPoint) / gradientLength.toFloat()
        return points[gradientStartPoint]!!.interpolate(points[gradientEndPoint]!!, gradientPercent)
    }
}