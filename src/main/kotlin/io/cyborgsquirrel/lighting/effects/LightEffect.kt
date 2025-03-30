package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.model.RgbColor

interface LightEffect {
    /**
     * Returns the name of the effect
     */
    fun getName(): String

    /**
     * Tells the effect to render the next frame and returns a list of [RgbColor] with a length matching the number of
     * LEDs specified in the effect's constructor
     */
    fun getNextStep(): List<RgbColor>

    /**
     * Returns the current settings for the LightEffect
     */
    fun getSettings(): Any

    /**
     * Informs the light effect it should fade to black or stop repeating after completing the next iteration
     */
    fun complete()

    /**
     * Boolean getter which returns true if the light effect has done after calling complete()
     * If complete() is not called, this will always be false.
     */
    fun isDone(): Boolean

    /**
     * Returns the number of times the effect has played
     */
    fun getIterations(): Int
}