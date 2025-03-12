package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effects.settings.NightriderColorFillEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.NightriderCometEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.NightriderEffectSettings
import io.cyborgsquirrel.lighting.enums.FadeCurve
import io.cyborgsquirrel.model.color.RgbColor
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
    private val settings: NightriderEffectSettings
) : LightEffect {

    private var frame: Long = 0
    private var reflect = false
    private var previousLocation = 0
    private var location = 0
    private var iterations = 0

    override fun getName(): String {
        return when (settings) {
            is NightriderColorFillEffectSettings -> LightEffectConstants.NIGHTRIDER_COLOR_FILL_NAME
            is NightriderCometEffectSettings -> LightEffectConstants.NIGHTRIDER_COMET_NAME
        }
    }

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
                rgbList.addAll(getBeginningColors(RgbColor.Blank))
            }

            // The comet + trail behind it
            val dotScaleFactor = 1.5f
            val dotColor = getColor(iterations).scale(dotScaleFactor)

            val cometBuffer = mutableListOf<RgbColor>()
            // Brightest spot is at the beginning for the reflect scenario
            if (reflect) {
                cometBuffer.add(dotColor)
            }

            for (i in 0..<settings.trailLength) {
                val interpolationFactor = when (settings.trailFadeCurve) {
                    FadeCurve.Linear -> i.toFloat() / settings.trailLength
                    FadeCurve.Logarithmic -> max(
                        log(
                            ((i + 1).toFloat() / settings.trailLength) * settings.trailLength,
                            settings.trailLength.toFloat()
                        ), 0f
                    )
                }
                val interpolatedColor = if (reflect) dotColor.interpolate(
                    RgbColor.Blank,
                    interpolationFactor
                ) else RgbColor.Blank.interpolate(dotColor, interpolationFactor)
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

            completeFrame(rgbList, RgbColor.Blank)
        } else {
            logger.warn("Config mismatch! Expected NightriderCometEffectSettings.")
            renderNightriderColorFill()
        }
    }

    private fun renderNightriderColorFill(): List<RgbColor> {
        val rgbList = mutableListOf<RgbColor>()
        val startingColor = if (reflect) getColor(iterations - 1) else getColor(iterations)
        val endingColor = if (iterations > 0) {
            if (reflect) getColor(iterations) else getColor(iterations - 1)
        } else {
            RgbColor.Blank
        }

        // Before scrolling dot
        rgbList.addAll(getBeginningColors(startingColor))

        // The scrolling dot + trail behind it
        val dotColor = getColor(iterations).scale(1.5f)
        rgbList.add(dotColor)
        rgbList.add(dotColor)

        return completeFrame(rgbList, endingColor)
    }

    override fun getSettings(): NightriderEffectSettings {
        return settings
    }

    override fun complete() {
        TODO("Not yet implemented")
    }

    override fun isDone(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getIterations(): Int {
        return iterations
    }

    private fun getColor(iteration: Int): RgbColor {
        return settings.getColors()[iteration % settings.getColors().size]
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

    private fun getBeginningColors(color: RgbColor): MutableList<RgbColor> {
        val rgbList = mutableListOf<RgbColor>()
        val upperBound = if (reflect) location else previousLocation
        for (i in 0..<upperBound) {
            rgbList.add(color)
        }

        return rgbList
    }

    private fun completeFrame(frameData: MutableList<RgbColor>, endColor: RgbColor): MutableList<RgbColor> {
        previousLocation = location
        frame++
        if (frameData.size < numberOfLeds) {
            for (i in 0..<numberOfLeds - frameData.size) {
                frameData.add(endColor)
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