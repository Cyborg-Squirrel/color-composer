package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effects.settings.SpectrumEffectSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import kotlin.math.ceil

class SpectrumLightEffect(
    private val numberOfLeds: Int,
    private val settings: SpectrumEffectSettings
) : LightEffect {

    private var frame = 0
    private var iterations = 0
    private var colorWidth = if (settings.colorPixelWidth == 0) settings.colorList.size else settings.colorPixelWidth
    private val colorList = settings.colorList
    private var referenceFrame = mutableListOf<RgbColor>()

    override fun getName(): String {
        return NAME
    }

    override fun getNextStep(): List<RgbColor> {
        val rgbList = mutableListOf<RgbColor>()
        val repeatOfColorsCount = ceil((numberOfLeds.toFloat() / colorWidth)).toInt()

        if (referenceFrame.isEmpty()) {
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
            frame++
            return rgbList
        } else {
            if (!settings.animated) {
                iterations++
                frame++
                return referenceFrame
            }

            rgbList.addAll(referenceFrame)
        }

        if (frame % numberOfLeds != 0) {
            val shiftedFrame = shift(rgbList, (frame % rgbList.size))
            frame++
            return shiftedFrame
        }

        iterations++
        frame++
        return rgbList
    }

    override fun getSettings() = settings

    override fun complete() {
        TODO("Not yet implemented")
    }

    override fun isDone(): Boolean {
        TODO("Not yet implemented")
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
        private const val NAME = LightEffectConstants.SPECTRUM_NAME
    }
}