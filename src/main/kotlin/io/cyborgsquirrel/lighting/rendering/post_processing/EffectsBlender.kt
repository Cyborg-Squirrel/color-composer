package io.cyborgsquirrel.lighting.rendering.post_processing

import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.model.RgbColor

class EffectsBlender {

    /**
     * Clears [accumulator] so a new frame's blend can start from blank. Mutates in place — the same accumulator buffer
     * is reused across frames to avoid per-frame allocation.
     */
    fun reset(accumulator: Array<RgbColor>) {
        for (color in accumulator) {
            color.setBlank()
        }
    }

    /**
     * Folds a single effect's [effect] buffer into the running [accumulator] for the given [blendMode]. Effects must be
     * folded in priority order (lowest to highest) so [BlendMode.Layer] lets the highest-priority effect win.
     * [index] is the zero-based position of this effect among the effects being blended.
     *
     * The accumulator is mutated in place; no buffers are allocated.
     */
    fun fold(
        blendMode: BlendMode,
        accumulator: Array<RgbColor>,
        effect: Array<RgbColor>,
        index: Int,
    ) {
        when (blendMode) {
            BlendMode.Additive -> {
                for (i in accumulator.indices) {
                    val rgbColor = effect[i]
                    if (!rgbColor.isBlank()) {
                        val acc = accumulator[i]
                        acc.red = minOf(acc.red + rgbColor.red, UByte.MAX_VALUE.toUInt()).toUByte()
                        acc.green = minOf(acc.green + rgbColor.green, UByte.MAX_VALUE.toUInt()).toUByte()
                        acc.blue = minOf(acc.blue + rgbColor.blue, UByte.MAX_VALUE.toUInt()).toUByte()
                    }
                }
            }

            BlendMode.Average -> {
                // Incremental (running-count) mean: acc += (effect - acc) / (index + 1). After all `count` effects this
                // equals the arithmetic mean up to per-step integer rounding. Signed Int math since the delta can be
                // negative.
                val divisor = index + 1
                for (i in accumulator.indices) {
                    val acc = accumulator[i]
                    val rgbColor = effect[i]
                    acc.red = (acc.red.toInt() + (rgbColor.red.toInt() - acc.red.toInt()) / divisor).toUByte()
                    acc.green = (acc.green.toInt() + (rgbColor.green.toInt() - acc.green.toInt()) / divisor).toUByte()
                    acc.blue = (acc.blue.toInt() + (rgbColor.blue.toInt() - acc.blue.toInt()) / divisor).toUByte()
                }
            }

            BlendMode.Layer -> {
                for (i in accumulator.indices) {
                    val acc = accumulator[i]
                    val rgbColor = effect[i]
                    if (rgbColor.red != 0.toUByte()) acc.red = rgbColor.red
                    if (rgbColor.green != 0.toUByte()) acc.green = rgbColor.green
                    if (rgbColor.blue != 0.toUByte()) acc.blue = rgbColor.blue
                }
            }

            BlendMode.UseHighest -> {
                for (i in accumulator.indices) {
                    val acc = accumulator[i]
                    val rgbColor = effect[i]
                    if (rgbColor.red > acc.red) acc.red = rgbColor.red
                    if (rgbColor.green > acc.green) acc.green = rgbColor.green
                    if (rgbColor.blue > acc.blue) acc.blue = rgbColor.blue
                }
            }
        }
    }
}
