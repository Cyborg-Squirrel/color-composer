package io.cyborgsquirrel.lighting.power_limits

import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.lighting.model.RgbColor

interface PowerLimiterService {

    fun setLimit(stripUuid: String, milliamps: Int)

    fun getLimit(stripUuid: String): Int?

    fun removeLimit(stripUuid: String)

    fun applyLimit(rgbList: List<RgbColor>, stripUuid: String): List<RgbColor>

    fun getDefaultBrightness(strip: LedStripEntity): Int
}