package io.cyborgsquirrel.lighting.power_limits

import io.cyborgsquirrel.lighting.model.LedStrip
import io.cyborgsquirrel.lighting.model.RgbColor
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.lang.Float.min

@Singleton
class PowerLimiterServiceImpl : PowerLimiterService {
    private var powerLimits = mutableMapOf<String, Int>()

    override fun setLimit(stripUuid: String, milliamps: Int) {
        logger.info("Setting power limit of ${milliamps}mA for strip $stripUuid")
        powerLimits[stripUuid] = milliamps
    }

    override fun getLimit(stripUuid: String): Int? = powerLimits[stripUuid]

    override fun removeLimit(stripUuid: String) {
        powerLimits.remove(stripUuid)
    }

    override fun applyLimit(rgbList: List<RgbColor>, stripUuid: String): List<RgbColor> {
        val powerLimit = powerLimits[stripUuid] ?: 0
        if (powerLimit > 0) {
            logger.debug("Applying ${powerLimit}mA limit to strip $stripUuid")
            var powerUsageMilliamps = 0f
            val powerPerRgbLed = POWER_PER_LED * 3
            for (rgb in rgbList) {
                val ledPower =
                    ((rgb.red + rgb.green + rgb.blue).toFloat() / (RGB_MAX_VALUE * 3u).toFloat()) * powerPerRgbLed
                powerUsageMilliamps += ledPower
            }

            logger.debug("Power usage of frame being sent to strip $stripUuid - ${powerUsageMilliamps}mA")
            if (powerUsageMilliamps >= powerLimit) {
                val scaleFactor = min(powerLimit / powerUsageMilliamps, 0.99f)
                logger.debug("Dimming LEDs to ${scaleFactor * 100}%")
                return rgbList.map {
                    it.scale(scaleFactor)
                }
            }
        }

        return rgbList
    }

    override fun getDefaultBrightness(strip: LedStrip): Int {
        val powerLimit = strip.getPowerLimitMilliAmps()
        return if (powerLimit != null) {
            powerLimit / strip.getLength() * POWER_PER_LED * 3
        } else {
            500 / strip.getLength() * POWER_PER_LED * 3
        }
    }

    companion object {
        // Max power output of each LED in milliamps
        private const val POWER_PER_LED = 20
        private const val RGB_MAX_VALUE = 255u
        private val logger = LoggerFactory.getLogger(PowerLimiterServiceImpl::class.java)
    }
}