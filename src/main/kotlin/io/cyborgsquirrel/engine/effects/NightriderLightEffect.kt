package io.cyborgsquirrel.engine.effects

import io.cyborgsquirrel.model.color.RgbColor

/**
 * Light effect where a light travels from one end of the strip to the other
 * changing the color behind it to the next color.
 */
class NightriderLightEffect(
    numberOfLeds: Int,
    private val colors: List<RgbColor>,
    private val colorScaleFactor: Float = 1.0f,
    private val pointerColorScaleFactor: Float = 1.0f
) :
    LightEffect(UUID, NAME, numberOfLeds) {

    private var frame: Long = 0
    private var reflect = false
    private var previousLocation = 0
    private var location = 0
    private var iterations = 0

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
            rgbList.add(scaleColor(colorScaleFactor, color))
        }

        // Location and previous location
        rgbList.add(getColor(iterations))
        rgbList.add(getColor(iterations))

        // After pointer
        if (iterations >= 1) {
            val lowerBound = if (reflect) previousLocation + 1 else location + 1
            for (i in lowerBound..<numberOfLeds) {
                val color = if (reflect) getColor(iterations) else getColor(iterations - 1)
                rgbList.add(scaleColor(colorScaleFactor, color))
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

    override fun getIterations(): Int {
        return iterations
    }

    private fun getColor(iteration: Int): RgbColor {
        return colors[iteration % colors.size]
    }

    private fun scaleColor(scaleFactor: Float, color: RgbColor): RgbColor {
        val red = (color.red.toInt() * scaleFactor).toInt().toUByte()
        val green = (color.green.toInt() * scaleFactor).toInt().toUByte()
        val blue = (color.blue.toInt() * scaleFactor).toInt().toUByte()
        return RgbColor(red, green, blue)
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
        // Randomly generated UUID
        const val UUID = "0E9B4727-3F0B-4D4D-8052-F9996333F21F"
        const val NAME = "Nightrider"
    }
}