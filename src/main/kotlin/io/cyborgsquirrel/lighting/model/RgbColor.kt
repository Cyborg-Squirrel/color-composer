package io.cyborgsquirrel.lighting.model

import io.micronaut.serde.annotation.Serdeable
import kotlin.math.min

/**
 * A single RGB value
 */
@Serdeable
class RgbColor(var red: UByte, var green: UByte, var blue: UByte) {

    /**
     * Scales the rgb values of this [RgbColor] by [scaleFactor]. A [scaleFactor] less than 1.0 will reduce all rgb values
     * a [scaleFactor] over 1.0 will increase all rgb values, a [scaleFactor] of zero will set the rgb values to zero.
     */
    fun scale(scaleFactor: Float): RgbColor {
        red = min(red.toShort() * scaleFactor, 255f).toUInt().toUByte()
        green = min(green.toShort() * scaleFactor, 255f).toUInt().toUByte()
        blue = min(blue.toShort() * scaleFactor, 255f).toUInt().toUByte()
        return this
    }

    /**
     * Interpolates this color and [otherColor].
     * [interpolation] is a value between 0 and 1 which determines how close the new color should be to this color or
     * the other color. A value of 0 will return an exact copy of this color and 1 will return an exact copy of the
     * [otherColor].
     */
    fun interpolate(otherColor: RgbColor, interpolation: Float): RgbColor {
        red = interpolate(red.toInt(), otherColor.red.toInt(), interpolation).toUInt().toUByte()
        green = interpolate(green.toInt(), otherColor.green.toInt(), interpolation).toUInt().toUByte()
        blue = interpolate(blue.toInt(), otherColor.blue.toInt(), interpolation).toUInt().toUByte()
        return this
    }

    fun setBlank() {
        red = 0.toUByte()
        green = 0.toUByte()
        blue = 0.toUByte()
    }

    fun isBlank(): Boolean {
        return red.toInt() == 0 && green.toInt() == 0 && blue.toInt() == 0
    }

    operator fun plus(other: RgbColor): RgbColor {
        // Saturating add — channels clamp at the max value instead of wrapping around (e.g. additive blending).
        val max = UByte.MAX_VALUE.toUInt()
        return RgbColor(
            minOf(red + other.red, max).toUByte(),
            minOf(green + other.green, max).toUByte(),
            minOf(blue + other.blue, max).toUByte(),
        )
    }

    operator fun div(denominator: UInt): RgbColor {
        return RgbColor((red / denominator).toUByte(), (green / denominator).toUByte(), (blue / denominator).toUByte())
    }

    override fun equals(other: Any?): Boolean {
        return if (other is RgbColor) {
            red == other.red && green == other.green && blue == other.blue
        } else false
    }

    override fun hashCode(): Int {
        var result = red.hashCode()
        result = 31 * result + green.hashCode()
        result = 31 * result + blue.hashCode()
        return result
    }

    fun copy() = RgbColor(red, green, blue)

    fun copyFrom(other: RgbColor) {
        red = other.red
        green = other.green
        blue = other.blue
    }

    private fun interpolate(a: Int, b: Int, interpolation: Float): Int {
        return (a - (a - b) * interpolation).toInt()
    }
}