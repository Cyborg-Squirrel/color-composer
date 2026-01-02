package io.cyborgsquirrel.lighting.model

import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.lighting.enums.BlendMode

/**
 * LED strip pool model
 * [strips] the list of member strips in order
 */
data class LedStripPoolModel(
    override val name: String,
    override val uuid: String,
    val poolType: PoolType,
    val strips: List<SingleLedStripModel>,
    override val blendMode: BlendMode,
): LedStripModel(uuid, name, blendMode) {
    override fun length(): Int {
        if (poolType == PoolType.Sync) {
            return strips.first().length
        }

        return strips.sumOf { it.length }
    }
}