package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effects.settings.FlameEffectSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Fire effect inspired by David Plummer's fire effect tutorial
 *
 * https://github.com/davepl/DavesGarageLEDSeries/blob/master/LED%20Episode%2011/include/fire.h
 */
class FlameLightEffect(
    private val numberOfLeds: Int,
    override val settings: FlameEffectSettings,
    override var palette: ColorPalette?,
) : LightEffect(settings, palette) {

    // TODO how do we count iterations? Do we count iterations for this effect?
    private var iterations = 0
    private val heat = IntArray(numberOfLeds)
    private var buffer = listOf<RgbColor>()

    override fun getNextStep(): List<RgbColor> {
        buffer = drawFire()
        return buffer
    }

    override fun getBuffer(): List<RgbColor> = buffer

    override fun getIterations() = iterations

    override fun updatePalette(palette: ColorPalette) {
        this.palette = palette
    }

    private fun drawFire(): List<RgbColor> {
        // Cool each cell
        for (i in heat.indices) {
            heat[i] = max(0, heat[i] - Random.nextInt(0, ((settings.cooling * 10) / heat.size) + 2))
        }

        // Diffuse heat
        for (i in heat.indices) {
            heat[i] = ((heat[i] * BLEND_SELF +
                    heat[(i + 1) % heat.size] * BLEND_NEIGHBOR_1 +
                    heat[(i + 2) % heat.size] * BLEND_NEIGHBOR_2 +
                    heat[(i + 3) % heat.size] * BLEND_NEIGHBOR_3) / BLEND_TOTAL)
        }

        // Ignite new sparks
        for (i in 0 until settings.sparks) {
            if (Random.nextInt(255) < settings.sparking) {
                val y = heat.size - 1 - Random.nextInt(settings.sparkHeight)
                heat[y] = Random.nextInt(200, 255)
            }
        }

        // Convert heat to color
        val rgbList = mutableListOf<RgbColor>()
        for (i in heat.indices) {
            val color = getColor(heat[heat.size - 1 - i], i)
            rgbList.add(color)
        }

        return rgbList
    }

    private fun getColor(heat: Int, index: Int): RgbColor {
        if (palette != null) {
            val ubyteMaxAsInt = UByte.MAX_VALUE.toInt()
            val heatVal = min(heat, ubyteMaxAsInt)
            val primary = palette!!.getPrimaryColor(index)
            val secondary = palette!!.getSecondaryColor(index)

            return if (heatVal > 170) {
                // Hottest
                secondary.interpolate(RgbColor.White, (heatVal / (255 - 170)).toFloat())
            } else if (heatVal > 85) {
                // Medium heat
                primary.interpolate(secondary, (heatVal / (170 - 85)).toFloat())
            } else {
                // Coolest
                primary.interpolate(RgbColor.Blank, (heatVal / 85).toFloat())
            }
        } else {
            return flameColorFromInt(heat)
        }
    }

    private fun flameColorFromInt(heat: Int): RgbColor {
        val ubyteMaxAsInt = UByte.MAX_VALUE.toInt()
        val heatVal = min(heat, ubyteMaxAsInt)
        val red: UByte
        val green: UByte
        val blue: UByte

        if (heatVal > 170) {
            red = UByte.MAX_VALUE
            green = UByte.MAX_VALUE
            blue = (((heatVal - 170) / 170.toFloat()) * ubyteMaxAsInt).toInt().toUByte()
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
