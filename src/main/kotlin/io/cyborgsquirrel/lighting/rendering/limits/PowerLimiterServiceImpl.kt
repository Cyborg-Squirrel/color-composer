package io.cyborgsquirrel.lighting.rendering.limits

import io.cyborgsquirrel.model.color.RgbColor
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class PowerLimiterServiceImpl : PowerLimiterService {
    private var powerLimit = 0

    override fun setLimit(milliamps: Int) {
        this.powerLimit = milliamps
    }

    override fun getLimit(): Int {
        return this.powerLimit
    }

    override fun applyLimit(rgbList: List<RgbColor>): List<RgbColor> {
        if (powerLimit > 0) {
            logger.debug("Applying ${powerLimit}mA limit")
            var powerUsageMilliamps = 0f
            val powerPerRgbLed = POWER_PER_LED * 3
            for (rgb in rgbList) {
                val ledPower =
                    ((rgb.red + rgb.green + rgb.blue).toFloat() / (RGB_MAX_VALUE * 3u).toFloat()) * powerPerRgbLed
                powerUsageMilliamps += ledPower
            }

            if (powerUsageMilliamps > powerLimit) {
                val scaleFactor = (powerLimit / powerUsageMilliamps)
                logger.debug("Dimming LEDs to ${scaleFactor * 100}%")
                return rgbList.map {
                    it.scale(scaleFactor)
                }
            }
        }

        return rgbList
    }

    companion object {
        // Max power output of each LED in milliamps
        private const val POWER_PER_LED = 20
        private const val RGB_MAX_VALUE = 255u
        private val logger = LoggerFactory.getLogger(PowerLimiterServiceImpl::class.java)
    }
}