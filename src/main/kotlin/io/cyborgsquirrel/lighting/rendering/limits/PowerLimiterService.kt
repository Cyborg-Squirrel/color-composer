package io.cyborgsquirrel.lighting.rendering.limits

import io.cyborgsquirrel.model.color.RgbColor

interface PowerLimiterService {

    fun setLimit(milliamps: Int)

    fun getLimit(): Int

    fun applyLimit(rgbList: List<RgbColor>): List<RgbColor>
}