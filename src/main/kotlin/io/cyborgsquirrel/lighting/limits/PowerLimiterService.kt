package io.cyborgsquirrel.lighting.limits

import io.cyborgsquirrel.lighting.model.RgbColor

interface PowerLimiterService {

    fun setLimit(stripUuid: String, milliamps: Int)

    fun getLimit(stripUuid: String): Int?

    fun removeLimit(stripUuid: String)

    fun applyLimit(rgbList: List<RgbColor>, stripUuid: String): List<RgbColor>
}