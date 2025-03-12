package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effects.settings.FlameEffectSettings
import io.cyborgsquirrel.model.color.RgbColor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Fire effect inspired by David Plummer's fire effect tutorial
 *
 * https://github.com/davepl/DavesGarageLEDSeries/blob/master/LED%20Episode%2011/include/fire.h
 */
class FlameLightEffect(private val numberOfLeds: Int, private val settings: FlameEffectSettings) : LightEffect {

    private var iterations = 0
    private val heat = IntArray(numberOfLeds + 10)
    private val cooling: Int = 30
    private val sparking: Int = 140
    private val sparks: Int = 3
    private val sparkHeight: Int = 4

    override fun getName(): String {
        return LightEffectConstants.FLAME_EFFECT_NAME
    }

    override fun getNextStep(): List<RgbColor> {
        val rgbList = drawFire()
//        iterations++
        return rgbList.subList(10, rgbList.size)
    }

    override fun getSettings(): FlameEffectSettings {
        return settings
    }

    override fun complete() {
        TODO("Not yet implemented")
    }

    override fun isDone(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getIterations(): Int {
        return iterations
    }

    private fun drawFire(): List<RgbColor> {
        // Cool each cell
        for (i in heat.indices) {
            heat[i] = max(0, heat[i] - Random.nextInt(0, ((cooling * 10) / heat.size) + 2))
//            heat[i] = heat[i] % 255
        }

        // Diffuse heat
        for (i in heat.indices) {
            heat[i] = ((heat[i] * BLEND_SELF +
                    heat[(i + 1) % heat.size] * BLEND_NEIGHBOR_1 +
                    heat[(i + 2) % heat.size] * BLEND_NEIGHBOR_2 +
                    heat[(i + 3) % heat.size] * BLEND_NEIGHBOR_3) / BLEND_TOTAL)
        }

        // Ignite new sparks
        for (i in 0 until sparks) {
            if (Random.nextInt(255) < sparking) {
                val y = heat.size - 1 - Random.nextInt(sparkHeight)
                heat[y] = (heat[y] + Random.nextInt(160, 255))
//                heat[y] = heat[y] % 255
                heat[y] = min(255, heat[y])
            }
        }

        // Convert heat to color
        val rgbList = mutableListOf<RgbColor>()
        for (i in heat.indices) {
            val color = colorFromInt(heat[heat.size - 1 - i])
            rgbList.add(color)
        }

        return rgbList
    }

    private fun colorFromInt(heat: Int): RgbColor {
        val ubyteMaxAsInt = UByte.MAX_VALUE.toInt()
        val heatVal = min(heat, ubyteMaxAsInt)
        val red: UByte
        val green: UByte
        val blue: UByte

        if (heatVal > 255) {
            red = UByte.MAX_VALUE
            green = UByte.MAX_VALUE
            blue = (((heatVal - 250) / 125.toFloat()) * ubyteMaxAsInt).toInt().toUByte()
        } else if (heatVal > 120) {
            red = UByte.MAX_VALUE
            green = (((heatVal - 120) / 120.toFloat()) * ubyteMaxAsInt).toInt().toUByte()
            blue = 0u
        } else {
            red = min(255, heatVal).toUByte()
            green = 0u
            blue = 0u
        }

        return RgbColor(red, green, blue)
    }

    companion object {
        private const val BLEND_SELF = 2
        private const val BLEND_NEIGHBOR_1 = 3
        private const val BLEND_NEIGHBOR_2 = 2
        private const val BLEND_NEIGHBOR_3 = 1
        private const val BLEND_TOTAL = BLEND_SELF + BLEND_NEIGHBOR_1 + BLEND_NEIGHBOR_2 + BLEND_NEIGHBOR_3
    }
}
