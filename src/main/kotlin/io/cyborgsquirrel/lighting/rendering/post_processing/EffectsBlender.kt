package io.cyborgsquirrel.lighting.rendering.post_processing

import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.model.RgbColor

class EffectsBlender {

    /**
     * Blends the RGB data from multiple effects into a single [RgbColor] buffer matching the length of the [strip]
     * [effectsRgbData] is must be sorted by priority highest to lowest for [BlendMode.Layer] to function correctly.
     */
    fun blendEffects(strip: LedStripModel, effectsRgbData: List<List<RgbColor>>): List<RgbColor> {
        // With a single effect every blend mode resolves to that effect's own buffer, so skip the per-pixel work.
        if (effectsRgbData.size == 1) {
            return effectsRgbData[0]
        }

        val length = strip.length()
        val renderedRgbData = ArrayList<RgbColor>(length)
        // Resolve the blend mode once rather than branching on every pixel.
        when (strip.blendMode) {
            BlendMode.Additive -> {
                for (i in 0..<length) {
                    var blended: RgbColor? = null
                    for (j in effectsRgbData.indices) {
                        val rgbColor = effectsRgbData[j][i]
                        if (!rgbColor.isBlank()) {
                            blended = if (blended == null) rgbColor else blended + rgbColor
                        }
                    }
                    renderedRgbData.add(blended ?: RgbColor.Blank)
                }
            }

            BlendMode.Average -> {
                for (i in 0..<length) {
                    var red = 0
                    var green = 0
                    var blue = 0
                    for (j in effectsRgbData.indices) {
                        val rgbColor = effectsRgbData[j][i]
                        red += rgbColor.red.toInt()
                        green += rgbColor.green.toInt()
                        blue += rgbColor.blue.toInt()
                    }

                    renderedRgbData.add(
                        RgbColor(
                            (red / effectsRgbData.size).toUByte(),
                            (green / effectsRgbData.size).toUByte(),
                            (blue / effectsRgbData.size).toUByte()
                        )
                    )
                }
            }

            BlendMode.Layer -> {
                for (i in 0..<length) {
                    var red = 0.toUByte()
                    var green = 0.toUByte()
                    var blue = 0.toUByte()
                    for (j in effectsRgbData.indices) {
                        val rgbColor = effectsRgbData[j][i]
                        if (rgbColor.red != 0.toUByte()) red = rgbColor.red
                        if (rgbColor.green != 0.toUByte()) green = rgbColor.green
                        if (rgbColor.blue != 0.toUByte()) blue = rgbColor.blue
                    }

                    renderedRgbData.add(RgbColor(red, green, blue))
                }
            }

            BlendMode.UseHighest -> {
                for (i in 0..<length) {
                    var red = 0.toUByte()
                    var green = 0.toUByte()
                    var blue = 0.toUByte()
                    for (j in effectsRgbData.indices) {
                        val rgbColor = effectsRgbData[j][i]
                        if (rgbColor.red > red) red = rgbColor.red
                        if (rgbColor.green > green) green = rgbColor.green
                        if (rgbColor.blue > blue) blue = rgbColor.blue
                    }

                    renderedRgbData.add(RgbColor(red, green, blue))
                }
            }
        }

        return renderedRgbData
    }
}