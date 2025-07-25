package io.cyborgsquirrel.lighting.model

import io.cyborgsquirrel.lighting.enums.BlendMode

/**
 * LED strip group model
 * [strips] the list of member strips in order
 */
data class LedStripGroupModel(
    private val name: String,
    private val uuid: String,
    private val strips: List<LedStripModel>,
    private val blendMode: BlendMode,
) : LedStrip {

    override fun getName(): String {
        return name
    }

    override fun getUuid(): String {
        return uuid
    }

    override fun getPin(): String {
        TODO("Not yet implemented")
    }

    override fun getLength(): Int {
        var len = 0
        for (strip in strips) {
            len += strip.getLength()
        }

        return len
    }

    override fun getHeight(): Int {
        // If a matrix LED is combined with a strip what should the height be? Should these two be allowed in the same group?
        TODO("Not yet implemented")
    }

    override fun getBlendMode(): BlendMode {
        return blendMode
    }

    // TODO
    override fun getBrightness(): Int = 0

    fun getStartingIndexOf(lightUuid: String): Int {
        var index = 0
        for (strip in strips) {
            if (lightUuid == strip.getUuid()) {
                // If index is greater than 0, subtract 1 to convert length
                // (counting starts at 1) to index (counting starts at 0)
                return if (index == 0) 0 else index - 1
            }

            index += strip.getLength()
        }

        // Strip with uuid [lightUuid] isn't in the group
        return -1
    }
}