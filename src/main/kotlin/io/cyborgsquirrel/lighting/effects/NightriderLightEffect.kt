package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effects.settings.NightriderLightEffectSettings
import io.cyborgsquirrel.model.color.RgbColor

/**
 * Light effect where a light travels from one end of the strip to the other
 * changing the color behind it to the next color.
 */
class NightriderLightEffect(
    numberOfLeds: Int,
    settings: NightriderLightEffectSettings
) : LightEffect(numberOfLeds, settings) {

    private var frame: Long = 0
    private var reflect = false
    private var previousLocation = 0
    private var location = 0
    private var iterations = 0
    private val colorList = settings.colorList

    override fun getName(): String {
        return NAME
    }

    override fun getNextStep(): List<RgbColor> {
        val reflectBefore = reflect
        reflect = shouldReflect()
        if (reflectBefore != reflect) {
            iterations++
        }

        updatePointerLocation()
        val rgbList = mutableListOf<RgbColor>()

        // Before pointer
        val upperBound = if (reflect) location else previousLocation
        for (i in 0..<upperBound) {
            val color = if (reflect) getColor(iterations - 1) else getColor(iterations)
            rgbList.add(color)
        }

        // Location and previous location
        rgbList.add(getColor(iterations).scale(1.5f))
        rgbList.add(getColor(iterations).scale(1.5f))

        // After pointer
        if (iterations >= 1) {
            val lowerBound = if (reflect) previousLocation + 1 else location + 1
            for (i in lowerBound..<numberOfLeds) {
                val color = if (reflect) getColor(iterations) else getColor(iterations - 1)
                rgbList.add(color)
            }
        } else {
            for (i in location + 1..<numberOfLeds) {
                rgbList.add(RgbColor.Blank)
            }
        }

        previousLocation = location
        frame++
        return rgbList
    }

    override fun complete() {
        TODO("Not yet implemented")
    }

    override fun done(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getIterations(): Int {
        return iterations
    }

    private fun getColor(iteration: Int): RgbColor {
        return colorList[iteration % colorList.size]
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
        private const val NAME = NIGHTRIDER_NAME
    }
}