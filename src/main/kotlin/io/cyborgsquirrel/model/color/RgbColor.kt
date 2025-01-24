package io.cyborgsquirrel.model.color

/**
 * A single RGB value
 */
data class RgbColor(val red: UByte, val green: UByte, val blue: UByte) {

    /**
     * Scales the rgb values of this [RgbColor] by [scaleFactor]. A [scaleFactor] less than 1.0 will reduce all rgb values
     * a [scaleFactor] over 1.0 will increase all rgb values, a [scaleFactor] of zero will set the rgb values to zero.
     */
    fun scale(scaleFactor: Float): RgbColor {
        val newRed = red.toShort() * scaleFactor
        val newGreen = green.toShort() * scaleFactor
        val newBlue = blue.toShort() * scaleFactor

        return RgbColor(newRed.toUInt().toUByte(), newGreen.toUInt().toUByte(), newBlue.toUInt().toUByte())
    }

    /**
     * Creates a new [RgbColor] between this color and [otherColor].
     * [interpolation] is a value between 0 and 1 which determines how close the new color should be to this color or
     * the other color. A value of 0 will return an exact copy of this color and 1 will return an exact copy of the
     * [otherColor].
     */
    fun interpolate(otherColor: RgbColor, interpolation: Float): RgbColor {
        val interpolatedRed = interpolate(red.toInt(), otherColor.red.toInt(), interpolation)
        val interpolatedGreen = interpolate(green.toInt(), otherColor.green.toInt(), interpolation)
        val interpolatedBlue = interpolate(blue.toInt(), otherColor.blue.toInt(), interpolation)

        return RgbColor(
            interpolatedRed.toUInt().toUByte(),
            interpolatedGreen.toUInt().toUByte(),
            interpolatedBlue.toUInt().toUByte(),
        )
    }

    private fun interpolate(a: Int, b: Int, interpolation: Float): Int {
        return (a - (a - b) * interpolation).toInt()
    }

    companion object {
        val Red = RgbColor(255u, 0u, 0u)
        val Orange = RgbColor(255u, 50u, 0u)
        val Yellow = RgbColor(255u, 160u, 0u)
        val Green = RgbColor(0u, 255u, 0u)
        val Blue = RgbColor(0u, 0u, 255u)
        val Cyan = RgbColor(0u, 217u, 255u)
        val Purple = RgbColor(128u, 0u, 255u)
        val Pink = RgbColor(255u, 0u, 150u)
        val White = RgbColor(255u, 255u, 255u)
        val Blank = RgbColor(0u, 0u, 0u)
        val Rainbow = listOf(Red, Orange, Yellow, Green, Blue, Purple)
    }
}