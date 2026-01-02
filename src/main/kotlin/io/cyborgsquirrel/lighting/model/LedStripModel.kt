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
 * [brightness] - the brightness 0-100
 */
data class LedStripModel(
    val name: String,
    val uuid: String,
    val pin: String,
    val length: Int,
    val height: Int,
    val blendMode: BlendMode,
    val brightness: Int
)