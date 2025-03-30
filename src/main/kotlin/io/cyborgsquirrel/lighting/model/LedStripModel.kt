package io.cyborgsquirrel.lighting.model

import io.cyborgsquirrel.lighting.enums.BlendMode

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
    private val height: Int,
    private val blendMode: BlendMode,
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

    override fun getHeight(): Int {
        return height
    }

    override fun getBlendMode(): BlendMode {
        return blendMode
    }
}