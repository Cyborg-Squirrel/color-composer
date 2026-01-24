package io.cyborgsquirrel.lighting.model

import io.cyborgsquirrel.lighting.enums.BlendMode

/**
 * LED strip model
 * [name] - the name of the LED strip
 * [uuid] - the strip's unique id
 * [pin] - the data pin on the client the strip is connected to
 * [length] - how many pixels are in the strip
 * [blendMode] - the mode for handling multiple effects running on the same strip if they overlap
 * [brightness] - the brightness 0-100
 * [clientUuid] - the uuid of the client this strip is connected to
 */
data class SingleLedStripModel(
    override val name: String,
    override val uuid: String,
    val pin: String,
    val length: Int,
    val height: Int,
    override val blendMode: BlendMode,
    val brightness: Int,
    val clientUuid: String,
    val inverted: Boolean,
) : LedStripModel(uuid, name, blendMode) {

    override fun length() = length

}
