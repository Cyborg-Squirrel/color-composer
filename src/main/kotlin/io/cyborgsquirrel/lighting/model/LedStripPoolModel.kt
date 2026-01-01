package io.cyborgsquirrel.lighting.model

import io.cyborgsquirrel.lighting.enums.BlendMode

/**
 * LED strip pool model
 * [strips] the list of member strips in order
 */
data class LedStripPoolModel(
    private val name: String,
    private val uuid: String,
    private val strips: List<LedStripModel>,
    private val blendMode: BlendMode,
) {
    fun getLength(): Int {
        var len = 0
        for (strip in strips) {
            len += strip.length
        }

        return len
    }
}