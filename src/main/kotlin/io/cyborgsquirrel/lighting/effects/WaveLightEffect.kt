package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effects.settings.WaveEffectSettings
import io.cyborgsquirrel.lighting.effects.helpers.EffectUpdateTickChecker
import io.cyborgsquirrel.lighting.effects.shared.Comet
import io.cyborgsquirrel.lighting.enums.Direction
import io.cyborgsquirrel.lighting.enums.FadeCurve
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.RgbColorPresets
import io.cyborgsquirrel.util.time.TimeHelper
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// TODO palette support
class WaveLightEffect(
    private val numberOfLeds: Int,
    override val settings: WaveEffectSettings,
    override var palette: ColorPalette?,
    timeHelper: TimeHelper,
) : LightEffect(settings, palette, timeHelper) {

    private var waveALocation = 0
    private var waveBLocation = 0
    private var frame = 0
    private var iterations = 0
    private lateinit var waveA: Comet
    private lateinit var waveB: Comet
    private val waveLength = settings.waveLength
    private val startPoint = (settings.startPointPercentage / 100.0 * numberOfLeds).toInt()
    private var buffer = Array(numberOfLeds) { RgbColorPresets.blank() }
    private val checker = EffectUpdateTickChecker(timeHelper)

    override fun render(): Array<RgbColor> {
        val updateDue = checker.isUpdateDue(settings.updatesPerSecond)
        if (!updateDue) return buffer
        val rgbData = ArrayList<RgbColor>(numberOfLeds)
        if (waveALocation <= -waveLength && waveBLocation >= numberOfLeds + waveLength) {
            iterations++
            waveALocation = startPoint - 1
            waveBLocation = startPoint
        } else if (frame == 0) {
            waveALocation = startPoint - 1
            waveBLocation = startPoint
            waveA = Comet(RgbColorPresets.red(), waveLength, FadeCurve.Logarithmic, Direction.HighToLow)
            waveB = Comet(RgbColorPresets.blue(), waveLength, FadeCurve.Logarithmic, Direction.LowToHigh)
        } else {
            if (waveALocation > -waveLength) waveALocation--
            if (waveBLocation < numberOfLeds + waveLength) waveBLocation++
        }

        val waveARgbData = waveA.buffer.subList(
            if (waveALocation > 0) 0 else abs(waveALocation), min(waveA.buffer.size, startPoint - waveALocation)
        )
        val waveBRgbData = waveB.buffer.subList(
            max(waveB.buffer.size - 1 - (waveBLocation - startPoint), 0), waveB.buffer.size
        )

        for (i in 0..<waveALocation) {
            rgbData.add(RgbColorPresets.blank())
        }

        rgbData.addAll(waveARgbData)

        for (i in 0..<waveBLocation - rgbData.size - waveBRgbData.size) {
            rgbData.add(RgbColorPresets.blank())
        }

        rgbData.addAll(waveBRgbData)

        for (i in 0..<numberOfLeds - rgbData.size) {
            rgbData.add(RgbColorPresets.blank())
        }

        frame++
        buffer = rgbData.toTypedArray()
        checker.onUpdate(timeHelper.millisSinceEpoch())
        return buffer
    }

    override fun getBuffer(): Array<RgbColor> = buffer

    override fun getIterations() = iterations

    override fun updatePalette(palette: ColorPalette) {
        this.palette = palette
    }
}