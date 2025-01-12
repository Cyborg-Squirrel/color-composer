package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.model.color.RgbColor
import kotlin.math.ceil

class RainbowLightEffect(
    numberOfLeds: Int,
    colors: List<RgbColor>,
) : LightEffect(UUID, NAME, numberOfLeds) {

    private var frame: Long = 0
    private var iterations = 0
    private var colorList = colors.ifEmpty {
        // Default color list
        RAINBOW
    }
    private var referenceFrame = listOf<RgbColor>()

    override fun getNextStep(): List<RgbColor> {
        val rgbList = mutableListOf<RgbColor>()
        val repeatOfColorsCount = ceil((numberOfLeds.toFloat() / colorList.size)).toInt()

        if (frame == 0L) {
            for (i in 0..<repeatOfColorsCount) {
                val color = colorList[i % colorList.size]
                val nextColor = colorList[(i + 1) % colorList.size]
                for (j in colorList.indices) {
                    val interpolationFactor = j.toFloat() / colorList.size
                    val interpolatedColor = color.interpolate(nextColor, interpolationFactor)
                    rgbList.add(interpolatedColor)

                    if (rgbList.size >= numberOfLeds) {
                        break
                    }
                }
            }

            referenceFrame = rgbList
        } else {
            rgbList.addAll(referenceFrame)
        }

        if (frame.toInt() % numberOfLeds != 0) {
            val shiftedFrame = shift(rgbList, (frame.toInt() % rgbList.size))
            frame++
            return shiftedFrame
        }

        iterations++
        frame++
        return rgbList
    }

    private fun <T> shift(list: List<T>, amount: Int): List<T> {
        val newList = mutableListOf<T>()
        for (i in list.indices) {
            newList.add(list[(i + amount) % list.size])
        }

        return newList
    }

    override fun getIterations(): Int {
        return iterations
    }

    companion object {
        // Randomly generated UUID
        private const val UUID = "67C64D65-159E-4F9C-9B43-60AFFF6C185B"
        private const val NAME = "Rainbow"
        private val RAINBOW =
            listOf(RgbColor.Red, RgbColor.Orange, RgbColor.Yellow, RgbColor.Green, RgbColor.Blue, RgbColor.Purple)
    }
}