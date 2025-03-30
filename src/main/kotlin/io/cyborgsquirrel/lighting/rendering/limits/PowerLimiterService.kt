package io.cyborgsquirrel.lighting.rendering.limits

import io.cyborgsquirrel.lighting.model.RgbColor

interface PowerLimiterService {

    fun setLimit(stripUuid: String, milliamps: Int)

    fun getLimit(stripUuid: String): Int?

    fun applyLimit(rgbList: List<RgbColor>, stripUuid: String): List<RgbColor>
}