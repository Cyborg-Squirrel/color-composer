package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effects.settings.SpectrumEffectSettings
import io.cyborgsquirrel.lighting.effects.shared.LightEffectHelper
import io.cyborgsquirrel.lighting.model.RgbColor
import kotlin.math.ceil

class SpectrumLightEffect(
    private val numberOfLeds: Int,
    private val settings: SpectrumEffectSettings,
    private var palette: ColorPalette?,
) : LightEffect {

    private var frame = 0
    private var iterations = 0
    private var referenceFrame = mutableListOf<RgbColor>()
    private val colorWidth = getColorWidth()
    private val helper = LightEffectHelper()
    private var buffer = listOf<RgbColor>()

    override fun getNextStep(): List<RgbColor> {
        val rgbList = mutableListOf<RgbColor>()
        val repeatOfColorsCount = ceil((numberOfLeds.toFloat() / colorWidth)).toInt()

        if (referenceFrame.isEmpty()) {
            for (i in 0..<repeatOfColorsCount) {
                val colors = colorList(i)
                val color = colors[i % colors.size]
                val nextColor = colors[(i + 1) % colors.size]
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
            buffer = rgbList
            return rgbList
        } else {
            if (!settings.animated) {
                iterations++
                frame++
                buffer = referenceFrame
                return referenceFrame
            }

            rgbList.addAll(referenceFrame)
        }

        if (frame % numberOfLeds != 0) {
            val shiftedFrame = helper.shift(rgbList, (frame % rgbList.size))
            frame++
            buffer = shiftedFrame
            return shiftedFrame
        }

        iterations++
        frame++
        buffer = rgbList
        return rgbList
    }

    override fun getBuffer(): List<RgbColor> = buffer

    override fun getSettings() = settings

    private fun colorList(index: Int): List<RgbColor> {
        if (palette != null) {
            val mainColors = listOf(palette!!.getPrimaryColor(index), palette!!.getSecondaryColor(index))
            val tertiary = palette!!.getTertiaryColor(index)
            val otherColors = palette!!.getOtherColors(index)
            return if (tertiary == null) {
                mainColors
            } else {
                mainColors + tertiary
            } + otherColors
        } else {
            return RgbColor.Rainbow
        }
    }

    private fun getColorWidth(): Int {
        return if (settings.colorPixelWidth <= 0) {
            colorList(1).size
        } else {
            settings.colorPixelWidth
        }
    }

    override fun getIterations() = iterations

    override fun updatePalette(palette: ColorPalette) {
        this.palette = palette
    }
}