package io.cyborgsquirrel.lighting.rendering.limits

import io.cyborgsquirrel.model.color.RgbColor

interface PowerLimiterService {

    fun setLimit(stripUuid: String, milliamps: Int)

    fun getLimit(stripUuid: String): Int?

    fun applyLimit(rgbList: List<RgbColor>, stripUuid: String): List<RgbColor>
}