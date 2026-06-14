package io.cyborgsquirrel.lighting.effects.shared

import io.cyborgsquirrel.lighting.enums.Direction
import io.cyborgsquirrel.lighting.enums.FadeCurve
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.RgbColorPresets
import kotlin.math.log
import kotlin.math.max

class Comet(val color: RgbColor, val length: Int, fadeCurve: FadeCurve, direction: Direction) {

    private val cometBuffer = ArrayList<RgbColor>(length)

    val buffer: MutableList<RgbColor>
        get() = cometBuffer

    init {
        for (i in 0..<length) {
            val interpolationFactor = when (fadeCurve) {
                FadeCurve.Linear -> i.toFloat() / length
                FadeCurve.Logarithmic -> max(
                    log(
                        ((i + 1).toFloat() / length) * length,
                        length.toFloat()
                    ), 0f
                )
            }
            val interpolatedColor = if (direction == Direction.HighToLow) color.interpolate(
                RgbColorPresets.blank(),
                interpolationFactor
            ) else RgbColorPresets.blank().interpolate(color, interpolationFactor)
            cometBuffer.add(interpolatedColor)
        }
    }
}