package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effects.settings.MarqueeEffectSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.util.shift
import io.cyborgsquirrel.util.time.TimeHelper

class MarqueeEffect(
    private val numberOfLeds: Int,
    override val settings: MarqueeEffectSettings,
    override var palette: ColorPalette?,
    private val timeHelper: TimeHelper,
) : LightEffect(settings, palette) {

    private var frame = 0
    private var iterations = 0
    private var lastChangeMillis = timeHelper.millisSinceEpoch()
    private var shiftAmount = 0
    private var buffer = listOf<RgbColor>()

    override fun getNextStep(): List<RgbColor> {
        val dotList = mutableListOf<Boolean>()
        val rgbList = mutableListOf<RgbColor>()
        var drawingDot = true
        var dotStart = 0
        var spaceStart = 0
        var done = false
        var i = 0

        while (!done) {
            if (drawingDot) {
                if (i - dotStart >= settings.dotLength) {
                    drawingDot = false
                    done = !dotList.first() && i >= numberOfLeds
                    spaceStart = i
                }
            } else {
                if (i - spaceStart >= settings.spaceBetweenDots) {
                    drawingDot = true
                    done = dotList.first() && i >= numberOfLeds
                    dotStart = i
                }
            }

            if (!done) {
                dotList.add(drawingDot)
            }

            i++
        }

        val nowMillis = timeHelper.millisSinceEpoch()
        if ((nowMillis - lastChangeMillis) / 1000f > 1 / settings.scrollAmountPerSecond.toFloat()) {
            shiftAmount = (shiftAmount + 1) % numberOfLeds
            lastChangeMillis = nowMillis
        }

        val shiftedDotList = dotList.shift(shiftAmount)

        for (indx in shiftedDotList.indices) {
            if (shiftedDotList[indx]) {
                rgbList.add(getColor(indx))
            } else {
                rgbList.add(RgbColor.Blank)
            }
        }

        frame++
        buffer = rgbList
        return rgbList
    }

    override fun getBuffer(): List<RgbColor> = buffer

    override fun getIterations() = iterations

    override fun updatePalette(palette: ColorPalette) {
        this.palette = palette
    }

    private fun getColor(index: Int): RgbColor {
        return if (palette != null) {
            palette!!.getPrimaryColor(index)
        } else {
            RgbColor.Cyan
        }
    }
}