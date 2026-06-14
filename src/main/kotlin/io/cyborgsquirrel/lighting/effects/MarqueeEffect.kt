package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effects.settings.MarqueeEffectSettings
import io.cyborgsquirrel.lighting.effects.helpers.EffectUpdateTickChecker
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.RgbColorPresets
import io.cyborgsquirrel.util.shift
import io.cyborgsquirrel.util.time.TimeHelper

class MarqueeEffect(
    private val numberOfLeds: Int,
    override val settings: MarqueeEffectSettings,
    override var palette: ColorPalette?,
    timeHelper: TimeHelper,
) : LightEffect(settings, palette, timeHelper) {

    private var frame = 0
    private var iterations = 0
    private var shiftAmount = 0
    private var buffer = Array(numberOfLeds) { RgbColorPresets.blank() }
    private val dotList = mutableListOf<Boolean>()
    private val checker = EffectUpdateTickChecker(timeHelper)

    override fun getNextStep(): Array<RgbColor> {
        val updateDue = checker.isUpdateDue(settings.updatesPerSecond)
        if (!updateDue) return buffer
        if (frame == 0) {
            initDotList()
        } else {
            shiftAmount = (shiftAmount + 1) % numberOfLeds
        }
        val shiftedDotList = dotList.shift(shiftAmount)

        for ((j, element) in buffer.withIndex()) {
            if (shiftedDotList[j]) {
                buffer[j].copyFrom(getColor(j))
            } else {
                element.setBlank()
            }
        }

        frame++
        checker.onUpdate(timeHelper.millisSinceEpoch())
        return buffer
    }

    private fun initDotList() {
        dotList.clear()
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
    }

    override fun getBuffer(): Array<RgbColor> = buffer

    override fun getIterations() = iterations

    override fun updatePalette(palette: ColorPalette) {
        this.palette = palette
    }

    private fun getColor(index: Int): RgbColor {
        return if (palette != null) {
            palette!!.getPrimaryColor(index)
        } else {
            RgbColorPresets.amber()
        }
    }
}
