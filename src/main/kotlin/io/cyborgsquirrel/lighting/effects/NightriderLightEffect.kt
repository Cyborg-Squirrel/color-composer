package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effects.settings.NightriderColorFillEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.NightriderCometEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.NightriderEffectSettings
import io.cyborgsquirrel.lighting.enums.FadeCurve
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.util.time.TimeHelper
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min

/**
 * Light effect where a light travels from one end of the strip to the other
 * changing the color behind it to the next color.
 */
class NightriderLightEffect(
    private val numberOfLeds: Int,
    override val settings: NightriderEffectSettings,
    override var palette: ColorPalette?,
    private val timeHelper: TimeHelper,
) : LightEffect(settings, palette) {

    private var frame: Long = 0
    private var reflect = false
    private var previousLocation = 0
    private var location = 0
    private var iterations = 0
    private var buffer = List(numberOfLeds) { RgbColor.Blank }
    private var lastChangeMillis = 0L

    override fun getNextStep(): List<RgbColor> {
        val nowMillis = timeHelper.millisSinceEpoch()
        if ((nowMillis - lastChangeMillis) / 1000f > 1 / settings.updatesPerSecond().toFloat()) {
            lastChangeMillis = nowMillis
            onNextStep()
        }

        buffer = when (settings) {
            is NightriderColorFillEffectSettings -> renderNightriderColorFill()
            is NightriderCometEffectSettings -> renderNightriderComet()
        }

        previousLocation = location
        frame++
        return buffer
    }

    override fun getBuffer(): List<RgbColor> = buffer

    private fun renderNightriderComet(): List<RgbColor> {
        return if (settings is NightriderCometEffectSettings) {
            val rgbList = mutableListOf<RgbColor>()

            if (location > 0) {
                // Before comet
                for (i in 0..<previousLocation) {
                    rgbList.add(RgbColor.Blank)
                }
            }

            // The comet + trail behind it
            val dotScaleFactor = 1.5f
            val dotColor = getColor(location, iterations).scale(dotScaleFactor)

            val cometBuffer = mutableListOf<RgbColor>()
            // Brightest spot is at the beginning for the reflect scenario
            if (reflect) {
                cometBuffer.add(dotColor)
            }

            for (i in 0..<settings.trailLength) {
                val color = getColor(i + cometBuffer.size + location, iterations).scale(dotScaleFactor)
                val interpolationFactor = when (settings.trailFadeCurve) {
                    FadeCurve.Linear -> min((i + 1).toFloat() / settings.trailLength, 1f)
                    FadeCurve.Logarithmic -> max(log(i + 1f, settings.trailLength.toFloat()), 0.05f)
                }
                val interpolatedColor = if (reflect) color.interpolate(
                    RgbColor.Blank, interpolationFactor
                ) else RgbColor.Blank.interpolate(color, interpolationFactor)
                cometBuffer.add(interpolatedColor)
            }

            // Brightest spot is the end for the non-reflect scenario
            if (!reflect) {
                cometBuffer.add(dotColor)
            }

            // Add the comet
            if (location < 0) {
                rgbList.addAll(cometBuffer.subList(abs(location), cometBuffer.size))
            } else {
                rgbList.addAll(cometBuffer)
            }

            for (i in rgbList.size..<buffer.size) {
                rgbList.add(RgbColor.Blank)
            }

            rgbList
        } else {
            logger.warn("Config mismatch! Expected NightriderCometEffectSettings.")
            renderNightriderColorFill()
        }
    }

    private fun renderNightriderColorFill(): List<RgbColor> {
        val rgbList = mutableListOf<RgbColor>()
        val brightnessScaling = if (settings is NightriderColorFillEffectSettings) settings.brightnessScaling else 1f
        for (i in 0..<previousLocation) {
            if (reflect) {
                rgbList.add(buffer[i])
            } else {
                rgbList.add(getColor(i, iterations).scale(brightnessScaling))
            }
        }

        // The scrolling dot + trail behind it
        rgbList.add(getColor(location, iterations))
        rgbList.add(getColor(location + 1, iterations))

        for (i in rgbList.size..<buffer.size) {
            if (reflect) {
                rgbList.add(getColor(i, iterations).scale(brightnessScaling))
            } else {
                rgbList.add(buffer[i])
            }
        }

        return rgbList
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
            return RgbColor.Rainbow[iteration % RgbColor.Rainbow.size]
        }
    }

    private fun updatePointerLocation() {
        if (settings.wrap()) {
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
        if (settings.wrap()) {
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