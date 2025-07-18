package io.cyborgsquirrel.lighting.model

import io.cyborgsquirrel.lighting.enums.BlendMode

/**
 * LED strip model
 * [name] - the name of the LED strip
 * [uuid] - the strip's unique id
 * [pin] - the data pin on the client the strip is connected to
 * [length] - how many pixels are in the strip
 * [reversed] - flag indicating whether RGB values should be reversed
 * [blendMode] - the mode for handling multiple effects running on the same strip if they overlap
 * [powerLimit] - the power limit in milliamps
 */
data class LedStripModel(
    private val name: String,
    private val uuid: String,
    private val pin: String,
    private val length: Int,
    private val height: Int,
    private val blendMode: BlendMode,
    private val powerLimit: Int?
) : LedStrip {

    override fun getName(): String = name

    override fun getUuid(): String = uuid

    override fun getPin(): String = pin

    override fun getLength(): Int = length

    override fun getHeight(): Int = height

    override fun getBlendMode(): BlendMode = blendMode

    override fun getPowerLimitMilliAmps(): Int? = powerLimit
}