package io.cyborgsquirrel.lighting.rendering.post_processing

import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.model.RgbColor

class EffectsBlender {

    fun blendEffects(strip: LedStripModel, effectsRgbData: List<List<RgbColor>>): MutableList<RgbColor> {
        val renderedRgbData = mutableListOf<RgbColor>()
        for (i in 0..<strip.length()) {
            when (strip.blendMode) {
                BlendMode.Additive -> {
                    var didAdd = false
                    for (j in effectsRgbData.indices) {
                        val rgbColor = effectsRgbData[j][i]
                        if (!rgbColor.isBlank()) {
                            if (didAdd) {
                                renderedRgbData[i] += rgbColor
                            } else {
                                didAdd = true
                                renderedRgbData.add(rgbColor)
                            }
                        }
                    }

                    if (!didAdd) {
                        renderedRgbData.add(RgbColor.Blank)
                    }
                }

                BlendMode.Average -> {
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

                BlendMode.Layer -> {
                    var red = 0.toUByte()
                    var green = 0.toUByte()
                    var blue = 0.toUByte()
                    for (j in effectsRgbData.indices) {
                        val rgbColor = effectsRgbData[j][i]
                        if (rgbColor.red != 0.toUByte()) {
                            red = rgbColor.red
                        }
                        if (rgbColor.green != 0.toUByte()) {
                            green = rgbColor.green
                        }
                        if (rgbColor.blue != 0.toUByte()) {
                            blue = rgbColor.blue
                        }
                    }

                    renderedRgbData.add(RgbColor(red, green, blue))
                }
            }
        }

        return renderedRgbData
    }
}