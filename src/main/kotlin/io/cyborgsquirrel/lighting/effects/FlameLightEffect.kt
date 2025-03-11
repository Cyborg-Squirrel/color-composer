package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effects.settings.FlameEffectSettings
import io.cyborgsquirrel.model.color.RgbColor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class FlameLightEffect(private val numberOfLeds: Int, private val settings: FlameEffectSettings) : LightEffect {

    private var iterations = 0
    private val flameBuffer = mutableListOf<RgbColor>()
    private val fire = FireEffect(numberOfLeds)

    override fun getName(): String {
        return LightEffectConstants.FLAME_EFFECT_NAME
    }

    override fun getNextStep(): List<RgbColor> {
        val rgbList = fire.drawFire()
//        iterations++
        return rgbList
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
}


class FireEffect(
    val size: Int,
    val cooling: Int = 20,
    val sparking: Int = 100,
    val sparks: Int = 3,
    val sparkHeight: Int = 4,
) {
    private val heat = IntArray(size)

    fun drawFire(): List<RgbColor> {
        val rgbList = mutableListOf<RgbColor>()

        // Cool each cell
        for (i in heat.indices) {
            heat[i] = max(0, heat[i] - Random.nextInt(0, ((cooling * 10) / size) + 2))
//            heat[i] = heat[i] % 255
        }

        // Diffuse heat
        for (i in heat.indices) {
            heat[i] = ((heat[i] * BLEND_SELF +
                    heat[(i + 1) % size] * BLEND_NEIGHBOR_1 +
                    heat[(i + 2) % size] * BLEND_NEIGHBOR_2 +
                    heat[(i + 3) % size] * BLEND_NEIGHBOR_3) / BLEND_TOTAL)
        }

        // Ignite new sparks
        for (i in 0 until sparks) {
            if (Random.nextInt(255) < sparking) {
                val y = size - 1 - Random.nextInt(sparkHeight)
                heat[y] = (heat[y] + Random.nextInt(160, 255))
//                heat[y] = heat[y] % 255
                heat[y] = min(255, heat[y])
            }
        }

        // Convert heat to color
        for (i in heat.indices) {
            val color = colorFromInt(heat[size - 1 - i])
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

        if (heatVal > 170) {
            red = UByte.MAX_VALUE
            green = UByte.MAX_VALUE
            blue = (((heatVal - 170) / 85.toFloat()) * ubyteMaxAsInt).toInt().toUByte()
        } else if (heatVal > 85) {
            red = UByte.MAX_VALUE
            green = (((heatVal - 85) / 85.toFloat()) * ubyteMaxAsInt).toInt().toUByte()
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
