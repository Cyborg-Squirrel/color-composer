package io.cyborgsquirrel.lighting.model

import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.lighting.enums.BlendMode

/**
 * LED strip pool model
 * [strips] the list of member strips in order
 */
data class LedStripPoolModel(
    val name: String,
    val uuid: String,
    val poolType: PoolType,
    val strips: List<SingleLedStripModel>,
    val blendMode: BlendMode,
): LedStripModel() {
    override fun uuid() = uuid

    override fun name() = name

    override fun length(): Int {
        if (poolType == PoolType.Sync) {
            return strips.first().length
        }

        return strips.sumOf { it.length }
    }

    override fun blendMode() = blendMode
}