package io.cyborgsquirrel.lighting.model

import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.lighting.enums.BlendMode

/**
 * LED strip pool model
 * [name] - the name of the LED strip
 * [uuid] - the strip's unique id
 * [length] - how many pixels are in the strip
 * [blendMode] - the mode for handling multiple effects running on the same strip if they overlap
 */
data class LedStripPoolModel(
    override val name: String,
    override val uuid: String,
    override val blendMode: BlendMode,
    val poolType: PoolType,
    val strips: List<SingleLedStripModel>,
): LedStripModel(uuid, name, blendMode) {

    override fun length(): Int {
        if (poolType == PoolType.Sync) {
            return strips.first().length
        }

        return strips.sumOf { it.length }
    }

    fun clientUuids(): List<String> {
        return strips.map { it.clientUuid }
    }

}