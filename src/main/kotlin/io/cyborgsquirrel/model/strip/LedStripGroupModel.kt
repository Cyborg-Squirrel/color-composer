package io.cyborgsquirrel.model.strip

/**
 * LED strip group model
 * [strips] the list of member strips in order
 */
data class LedStripGroupModel(
    private val name: String,
    private val uuid: String,
    private val strips: MutableList<LedStripModel>
) : LedStrip {

    override fun getName(): String {
        return name
    }

    override fun getUuid(): String {
        return uuid
    }

    override fun getLength(): Int {
        var len = 0
        for (strip in strips) {
            len += strip.getLength()
        }

        return len
    }

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