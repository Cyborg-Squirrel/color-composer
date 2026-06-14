package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effects.settings.NightriderColorFillEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.NightriderCometEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.NightriderEffectSettings
import io.cyborgsquirrel.lighting.effects.helpers.EffectUpdateTickChecker
import io.cyborgsquirrel.lighting.enums.FadeCurve
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.RgbColorPresets
import io.cyborgsquirrel.util.time.TimeHelper
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min

/**
 * Light effect where a light dot travels from one end of the strip to the other.
 * The LEDs where the dot previously traveled change color to match the dot, dimmed by [settings.brightnessScaling].
 * The dot reflects down the strip once it reaches the beginning or end of the strip.
 */
class NightriderLightEffect(
    private val numberOfLeds: Int,
    override val settings: NightriderEffectSettings,
    override var palette: ColorPalette?,
    timeHelper: TimeHelper,
) : LightEffect(settings, palette, timeHelper) {

    private var frame: Long = 0
    private var reflect = false
    private var previousLocation = 0
    private var location = 0
    private var iterations = 0
    private var buffer = Array(numberOfLeds) { RgbColorPresets.blank() }
    private val checker = EffectUpdateTickChecker(timeHelper)
    private var defaultColorsCache: List<RgbColor>? = null

    override fun render(): Array<RgbColor> {
        val updateDue = checker.isUpdateDue(settings.updatesPerSecond)
        if (!updateDue) return buffer
        onNextStep()

        when (settings) {
            is NightriderColorFillEffectSettings -> renderNightriderColorFill()
            is NightriderCometEffectSettings -> renderNightriderComet()
        }

        previousLocation = location
        frame++
        checker.onUpdate(timeHelper.millisSinceEpoch())
        return buffer
    }

    override fun getBuffer(): Array<RgbColor> = buffer

    private fun renderNightriderComet() {
        if (settings !is NightriderCometEffectSettings) {
            logger.warn("Config mismatch! Expected NightriderCometEffectSettings.")
            renderNightriderColorFill()
            return
        }

        // Blank everything before the comet.
        val start = if (location > 0) previousLocation else 0
        for (i in 0..<start) {
            buffer[i].setBlank()
        }

        // The comet + trail behind it
        val dotScaleFactor = 1.5f
        val dotColor = getColor(location, iterations).copy().scale(dotScaleFactor)

        val cometBuffer = ArrayList<RgbColor>(settings.trailLength + 2)
        // Brightest spot is at the beginning for the reflect scenario
        if (reflect) {
            cometBuffer.add(dotColor)
        }

        for (i in 0..<settings.trailLength) {
            val color = getColor(i + cometBuffer.size + location, iterations).copy().scale(dotScaleFactor)
            val interpolationFactor = when (settings.trailFadeCurve) {
                FadeCurve.Linear -> min((i + 1).toFloat() / settings.trailLength, 1f)
                FadeCurve.Logarithmic -> max(log(i + 1f, settings.trailLength.toFloat()), 0.05f)
            }
            val interpolatedColor = if (reflect) color.interpolate(
                RgbColorPresets.blank(), interpolationFactor
            ) else RgbColorPresets.blank().interpolate(color, interpolationFactor)
            cometBuffer.add(interpolatedColor)
        }

        // Brightest spot is the end for the non-reflect scenario
        if (!reflect) {
            cometBuffer.add(dotColor)
        }

        // Place the comet, clamped so its trailing portion can't extend past the end of the strip.
        val cometSource = if (location < 0) cometBuffer.subList(abs(location), cometBuffer.size) else cometBuffer
        var index = start
        for (color in cometSource) {
            if (index >= buffer.size) break
            buffer[index].copyFrom(color)
            index++
        }

        // Blank the remainder of the strip.
        for (i in index..<buffer.size) {
            buffer[i].setBlank()
        }
    }

    private fun renderNightriderColorFill() {
        val brightnessScaling = if (settings is NightriderColorFillEffectSettings) settings.brightnessScaling else 1f
        for (i in 0..<previousLocation) {
            if (!reflect) {
                buffer[i].copyFrom(getColor(i, iterations).copy().scale(brightnessScaling))
            }
        }

        // The scrolling dot (2px wide), clamped so it can't extend past the end of the strip.
        var index = previousLocation
        if (index < buffer.size) buffer[index].copyFrom(getColor(location, iterations))
        index++
        if (index < buffer.size) buffer[index].copyFrom(getColor(location + 1, iterations))
        index++

        for (i in index..<buffer.size) {
            if (reflect) {
                buffer[i].copyFrom(getColor(i, iterations).copy().scale(brightnessScaling))
            }
        }
    }

    override fun getIterations() = iterations

    override fun updatePalette(palette: ColorPalette) {
        this.palette = palette
    }

    private fun getColor(index: Int, iteration: Int): RgbColor {
        if (palette != null) {
            val mainColors = listOf(palette!!.getPrimaryColor(index), palette!!.getSecondaryColor(index))
            val tertiary = palette!!.getTertiaryColor(index)
            val otherColors = palette!!.getOtherColors(index)
            val allColors = if (tertiary == null) {
                mainColors
            } else {
                mainColors + tertiary
            } + otherColors

            return allColors[iteration % allColors.size]
        } else {
            if (defaultColorsCache == null) {
                defaultColorsCache = RgbColorPresets.rainbow()
            }

            return defaultColorsCache!![iteration % defaultColorsCache!!.size]
        }
    }

    private fun updatePointerLocation() {
        if (settings.wrap) {
            location++
            if (location >= numberOfLeds) {
                iterations++
                location %= numberOfLeds
                previousLocation = location
            }
            return
        }

        when (settings) {
            is NightriderColorFillEffectSettings -> {
                if (reflect && location > 0) {
                    location--
                }
            }

            is NightriderCometEffectSettings -> {
                if (reflect && location > 0 - settings.trailLength) {
                    location--
                }
            }
        }

        if (!reflect && location < numberOfLeds - 1) {
            location++
        }
    }

    private fun shouldReflect(): Boolean {
        if (settings.wrap) {
            return false
        }

        when (settings) {
            is NightriderColorFillEffectSettings -> {
                if (reflect && location == 0) {
                    return false
                }
            }

            is NightriderCometEffectSettings -> {
                if (reflect && location == 0 - settings.trailLength) {
                    return false
                }
            }
        }

        if (!reflect && location == numberOfLeds - 1) {
            return true
        }

        return reflect
    }

    private fun onNextStep() {
        val reflectBefore = reflect
        reflect = shouldReflect()
        if (reflectBefore != reflect) {
            iterations++
        }

        updatePointerLocation()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NightriderLightEffect::class.java)
    }
}
