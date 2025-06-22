package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effects.settings.ScrollingDotEffectSettings
import io.cyborgsquirrel.lighting.effects.shared.LightEffectHelper
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.util.time.TimeHelper

class ScrollingDotsEffect(
    private val numberOfLeds: Int,
    private val settings: ScrollingDotEffectSettings,
    private var palette: ColorPalette?,
    private val timeHelper: TimeHelper,
) : LightEffect {

    private var frame = 0
    private var iterations = 0
    private val helper = LightEffectHelper()
    private var lastChangeMillis = timeHelper.millisSinceEpoch()
    private var shiftAmount = 0

    override fun getNextStep(): List<RgbColor> {
        val rgbList = mutableListOf<RgbColor>()
        var drawingDot = true
        var dotStart = 0
        var spaceStart = 0

        for (i in 0..<numberOfLeds) {
            if (drawingDot) {
                if (i - dotStart >= settings.dotLength) {
                    drawingDot = false
                    spaceStart = i
                    rgbList.add(RgbColor.Blank)
                } else {
                    rgbList.add(getColor(i))
                }
            } else {
                if (i - spaceStart >= settings.spaceBetweenDots) {
                    drawingDot = true
                    dotStart = i
                    rgbList.add(getColor(i))
                } else {
                    rgbList.add(RgbColor.Blank)
                }
            }
        }

        val nowMillis = timeHelper.millisSinceEpoch()
        if ((nowMillis - lastChangeMillis) / 1000f > 1 / settings.scrollAmountPerSecond.toFloat()) {
            shiftAmount++
            lastChangeMillis = nowMillis
        }

        val shiftedList = helper.shift(rgbList, shiftAmount)
        frame++
        return shiftedList
    }

    override fun getSettings() = settings

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