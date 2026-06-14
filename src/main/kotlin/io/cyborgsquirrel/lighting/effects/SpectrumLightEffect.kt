package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effect_palette.palette.GradientColorPalette
import io.cyborgsquirrel.lighting.effects.settings.SpectrumEffectSettings
import io.cyborgsquirrel.lighting.effects.helpers.EffectUpdateTickChecker
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.RgbColorPresets
import io.cyborgsquirrel.util.time.TimeHelper
import kotlin.math.ceil

class SpectrumLightEffect(
    private val numberOfLeds: Int,
    override val settings: SpectrumEffectSettings,
    override var palette: ColorPalette?,
    timeHelper: TimeHelper,
) : LightEffect(settings, palette, timeHelper) {

    private var frame = 0
    private var iterations = 0
    private var shift = 0
    private val colorWidth = getColorWidth()
    private var buffer = Array(numberOfLeds) { RgbColorPresets.blank() }
    private val checker = EffectUpdateTickChecker(timeHelper)

    override fun render(): Array<RgbColor> {
        val updateDue = checker.isUpdateDue(settings.updatesPerSecond)
        if (!updateDue) return buffer
        // getNextStep always advances now (the renderer gates on isUpdateDue), and it has several exit paths, so
        // record the update once up front.
        checker.onUpdate(timeHelper.millisSinceEpoch())

        if (frame == 0) {
            buildSpectrum()
            frame++
            return buffer
        }

        if (!settings.animated) {
            iterations++
            frame++
            return buffer
        }

        // Rotating the buffer left by one LED per frame is lossless (it only reorders the existing colors), so the
        // spectrum is exactly back to its starting arrangement every numberOfLeds frames.
        shift = (shift + 1) % numberOfLeds
        rotateBufferLeft()
        if (shift == 0) iterations++
        frame++
        return buffer
    }

    override fun getBuffer(): Array<RgbColor> = buffer

    private fun rotateBufferLeft() {
        val first = buffer[0]
        for (i in 0..<numberOfLeds - 1) {
            buffer[i] = buffer[i + 1]
        }
        buffer[numberOfLeds - 1] = first
    }

    private fun buildSpectrum() {
        val repeatOfColorsCount = ceil((numberOfLeds.toFloat() / colorWidth)).toInt()
        for (i in 0..<repeatOfColorsCount) {
            val colors = colorList(i)
            val color = colors[i % colors.size]
            if (palette is GradientColorPalette) {
                if (i < numberOfLeds) {
                    // Copy rather than alias the palette color so rotating the buffer never reaches back into it.
                    buffer[i] = color.copy()
                }
            } else {
                val nextColor = colors[(i + 1) % colors.size]
                for (j in 0..<colorWidth) {
                    val index = i * colorWidth + j
                    if (index >= numberOfLeds) {
                        break
                    }
                    val interpolationFactor = j.toFloat() / colorWidth
                    // Interpolate off a copy so the shared palette color isn't mutated and every LED gets its own object.
                    val interpolatedColor = color.copy().interpolate(nextColor, interpolationFactor)
                    buffer[index] = interpolatedColor
                }
            }
        }
    }

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
            return RgbColorPresets.rainbow()
        }
    }

    private fun getColorWidth(): Int {
        return if (settings.colorBandPercentage <= 0) {
            colorList(1).size
        } else {
            // Never let the band collapse to zero, otherwise repeatOfColorsCount (ceil(n / colorWidth)) explodes.
            (settings.colorBandPercentage / 100.0 * numberOfLeds).toInt().coerceAtLeast(1)
        }
    }

    override fun getIterations() = iterations

    override fun updatePalette(palette: ColorPalette) {
        this.palette = palette
    }
}