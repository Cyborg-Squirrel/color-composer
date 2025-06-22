package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effects.settings.NightriderColorFillEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.NightriderCometEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.NightriderEffectSettings
import io.cyborgsquirrel.lighting.enums.FadeCurve
import io.cyborgsquirrel.lighting.model.RgbColor
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.log
import kotlin.math.max

/**
 * Light effect where a light travels from one end of the strip to the other
 * changing the color behind it to the next color.
 */
class NightriderLightEffect(
    private val numberOfLeds: Int,
    private val settings: NightriderEffectSettings,
    private var palette: ColorPalette?,
) : LightEffect {

    private var frame: Long = 0
    private var reflect = false
    private var previousLocation = 0
    private var location = 0
    private var iterations = 0

    override fun getNextStep(): List<RgbColor> {
        onNextStep()
        return when (settings) {
            is NightriderColorFillEffectSettings -> renderNightriderColorFill()
            is NightriderCometEffectSettings -> renderNightriderComet()
        }
    }

    private fun renderNightriderComet(): List<RgbColor> {
        return if (settings is NightriderCometEffectSettings) {
            val rgbList = mutableListOf<RgbColor>()

            if (location > 0) {
                // Before comet
                rgbList.addAll(getBeginningColors({ RgbColor.Blank }))
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
                val color = getColor(i, iterations).scale(dotScaleFactor)
                val interpolationFactor = when (settings.trailFadeCurve) {
                    FadeCurve.Linear -> i.toFloat() / settings.trailLength
                    FadeCurve.Logarithmic -> max(
                        log(
                            ((i + 1).toFloat() / settings.trailLength) * settings.trailLength,
                            settings.trailLength.toFloat()
                        ), 0f
                    )
                }
                val interpolatedColor = if (reflect) color.interpolate(
                    RgbColor.Blank,
                    interpolationFactor
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

            completeFrame(rgbList, { RgbColor.Blank })
        } else {
            logger.warn("Config mismatch! Expected NightriderCometEffectSettings.")
            renderNightriderColorFill()
        }
    }

    private fun renderNightriderColorFill(): List<RgbColor> {
        val rgbList = mutableListOf<RgbColor>()
        val startingColorCallback: (Int) -> RgbColor =
            { location -> if (reflect) getColor(location, iterations - 1) else getColor(location, iterations) }
        val endingColorCallback: (Int) -> RgbColor = { location ->
            if (iterations > 0) {
                if (reflect) getColor(location, iterations) else getColor(location, iterations - 1)
            } else {
                RgbColor.Blank
            }
        }

        // Before scrolling dot
        rgbList.addAll(getBeginningColors(startingColorCallback))

        // The scrolling dot + trail behind it
        rgbList.add(getColor(location, iterations).scale(1.5f))
        rgbList.add(getColor(location + 1, iterations).scale(1.5f))

        return completeFrame(rgbList, endingColorCallback)
    }

    override fun getSettings() = settings

    override fun getIterations(): Int {
        return iterations
    }

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

    private fun getBeginningColors(colorCallback: (Int) -> RgbColor): MutableList<RgbColor> {
        val rgbList = mutableListOf<RgbColor>()
        val upperBound = if (reflect) location else previousLocation
        for (i in 0..<upperBound) {
            rgbList.add(colorCallback(i))
        }

        return rgbList
    }

    private fun completeFrame(
        frameData: MutableList<RgbColor>,
        endingColorCallback: (Int) -> RgbColor
    ): MutableList<RgbColor> {
        previousLocation = location
        frame++
        if (frameData.size < numberOfLeds) {
            for (i in 0..<numberOfLeds - frameData.size) {
                frameData.add(endingColorCallback(i + frameData.size))
            }

            return frameData
        } else {
            return frameData.subList(0, numberOfLeds)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NightriderLightEffect::class.java)
    }
}