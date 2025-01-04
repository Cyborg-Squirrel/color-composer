package io.cyborgsquirrel.model.strip

/**
 * LED strip model
 * [name] - the name of the LED strip
 * [uuid] - the strip's unique id
 * [length] - how many pixels are in the strip
 * [reversed] - flag indicating whether RGB values should be reversed
 */
data class LedStripModel(
    private val name: String,
    private val uuid: String,
    private val length: Int,
    private val reversed: Boolean = false
) : LedStrip {

    override fun getName(): String {
        return name
    }

    override fun getUuid(): String {
        return uuid
    }

    override fun getLength(): Int {
        return length
    }

    override fun isReversed(): Boolean {
        return reversed
    }
}