package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effects.settings.DefaultNightriderEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.NightriderCometEffectSettings
import io.cyborgsquirrel.lighting.effects.settings.NightriderEffectSettings
import io.cyborgsquirrel.model.color.RgbColor
import org.slf4j.LoggerFactory

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
        return NAME
    }

    override fun getNextStep(): List<RgbColor> {
        return when (settings) {
            is DefaultNightriderEffectSettings -> renderNightriderDefault()
            is NightriderCometEffectSettings -> renderNightriderComet()
        }
    }

    private fun renderNightriderComet(): List<RgbColor> {
        return if (settings is NightriderCometEffectSettings) {
            val reflectBefore = reflect
            reflect = shouldReflect()
            if (reflectBefore != reflect) {
                iterations++
            }

            updatePointerLocation()
            val rgbList = mutableListOf<RgbColor>()

            // Before scrolling dot
            val upperBound = if (reflect) location else previousLocation
            for (i in 0..<upperBound) {
                rgbList.add(RgbColor.Blank)
            }

            // The scrolling dot + trail behind it
            val dotScaleFactor = 1.5f
            val dotColor = getColor(iterations).scale(dotScaleFactor)
            // Brightest spot is at the beginning for the reflect scenario
            if (reflect) {
                rgbList.add(dotColor)
            }

            for (i in 0..<settings.trailLength) {
                val interpolationFactor = i.toFloat() / settings.trailLength
//                    val interpolatedColor =
//                        dotColor.interpolate(if (reflect) startingColor else endingColor, interpolationFactor)
                val interpolatedColor = if (reflect) dotColor.interpolate(
                    RgbColor.Blank,
                    interpolationFactor
                ) else RgbColor.Blank.interpolate(dotColor, interpolationFactor)
                rgbList.add(interpolatedColor)
            }

            // Brightest spot is the end for the non-reflect scenario
            if (!reflect) {
                rgbList.add(dotColor)
            }

            // After scrolling dot
            if (iterations >= 1) {
                val lowerBound = if (reflect) previousLocation + 1 else location + 1
                for (i in lowerBound..<numberOfLeds) {
                    rgbList.add(RgbColor.Blank)
                }
            } else {
                for (i in location + 1..<numberOfLeds) {
                    rgbList.add(RgbColor.Blank)
                }
            }

            previousLocation = location
            frame++
            rgbList
        } else {
            logger.warn("Config mismatch! Expected NightriderCometEffectSettings.")
            renderNightriderDefault()
        }
    }

    private fun renderNightriderDefault(): List<RgbColor> {
        val reflectBefore = reflect
        reflect = shouldReflect()
        if (reflectBefore != reflect) {
            iterations++
        }

        updatePointerLocation()
        val rgbList = mutableListOf<RgbColor>()
        val startingColor = if (reflect) getColor(iterations - 1) else getColor(iterations)
        val endingColor = if (iterations >= 1) {
            if (reflect) getColor(iterations) else getColor(iterations - 1)
        } else {
            RgbColor.Blank
        }

        // Before scrolling dot
        val upperBound = if (reflect) location else previousLocation
        for (i in 0..<upperBound) {
            rgbList.add(startingColor)
        }

        // The scrolling dot + trail behind it
        val dotColor = getColor(iterations).scale(1.5f)
        rgbList.add(dotColor)
        rgbList.add(dotColor)

        // After scrolling dot
        if (iterations >= 1) {
            val lowerBound = if (reflect) previousLocation + 1 else location + 1
            for (i in lowerBound..<numberOfLeds) {
                rgbList.add(endingColor)
            }
        } else {
            for (i in location + 1..<numberOfLeds) {
                rgbList.add(endingColor)
            }
        }

        previousLocation = location
        frame++
        return rgbList
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
        if (reflect && location > 0) {
            location--
        }

        if (!reflect && location < numberOfLeds - 1) {
            location++
        }
    }

    private fun shouldReflect(): Boolean {
        if (reflect && location == 0) {
            return false
        }

        if (!reflect && location == numberOfLeds - 1) {
            return true
        }

        return reflect
    }

    companion object {
        private const val NAME = LightEffectConstants.NIGHTRIDER_NAME
        private val logger = LoggerFactory.getLogger(NightriderLightEffect::class.java)
    }
}