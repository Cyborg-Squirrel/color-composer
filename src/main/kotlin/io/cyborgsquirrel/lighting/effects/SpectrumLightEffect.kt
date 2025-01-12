package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.model.color.RgbColor
import kotlin.math.ceil

class SpectrumLightEffect(
    private val numberOfLeds: Int,
    colorPixelWidth: Int,
    colors: List<RgbColor>,
) : AnimatedSpectrumLightEffect(numberOfLeds, colorPixelWidth, colors) {

    private var iterations = 0
    private var colorList = colors.ifEmpty {
        // Default color list
        RAINBOW
    }
    private var colorWidth = if (colorPixelWidth == 0) colorList.size else colorPixelWidth
    private var referenceFrame = listOf<RgbColor>()

    override fun getUuid(): String {
        return UUID
    }

    override fun getName(): String {
        return NAME
    }

    override fun getNextStep(): List<RgbColor> {
        val rgbList = mutableListOf<RgbColor>()
        val repeatOfColorsCount = ceil((numberOfLeds.toFloat() / colorWidth)).toInt()

        if (iterations == 0) {
            for (i in 0..<repeatOfColorsCount) {
                val color = colorList[i % colorList.size]
                val nextColor = colorList[(i + 1) % colorList.size]
                for (j in 0..<colorWidth) {
                    val interpolationFactor = j.toFloat() / colorWidth
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

        iterations++
        return rgbList
    }

    override fun getIterations(): Int {
        return iterations
    }

    companion object {
        // Randomly generated UUID
        private const val UUID = "67C64D65-159E-4F9C-9B43-60AFFF6C185B"
        private const val NAME = "Spectrum"
        private val RAINBOW =
            listOf(RgbColor.Red, RgbColor.Orange, RgbColor.Yellow, RgbColor.Green, RgbColor.Blue, RgbColor.Purple)
    }
}