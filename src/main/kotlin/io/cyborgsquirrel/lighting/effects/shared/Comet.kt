package io.cyborgsquirrel.lighting.effects.shared

import io.cyborgsquirrel.lighting.enums.Direction
import io.cyborgsquirrel.lighting.enums.FadeCurve
import io.cyborgsquirrel.model.color.RgbColor
import kotlin.math.log
import kotlin.math.max

class Comet(val color: RgbColor, val length: Int, val fadeCurve: FadeCurve, val direction: Direction) {

    private val cometBuffer = mutableListOf<RgbColor>()

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
                RgbColor.Blank,
                interpolationFactor
            ) else RgbColor.Blank.interpolate(color, interpolationFactor)
            cometBuffer.add(interpolatedColor)
        }
    }
}