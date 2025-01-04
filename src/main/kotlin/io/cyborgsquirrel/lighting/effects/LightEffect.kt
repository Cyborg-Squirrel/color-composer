package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.model.color.RgbColor

abstract class LightEffect(val uuid: String, val name: String, val numberOfLeds: Int) {
    /**
     * Tells the effect to render the next frame and returns a buffer with a length of [numberOfLeds]
     */
    abstract fun getNextStep(): List<RgbColor>

    /**
     * Returns the number of times the effect has played
     */
    abstract fun getIterations(): Int
}