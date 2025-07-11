package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effects.settings.WaveEffectSettings
import io.cyborgsquirrel.lighting.effects.shared.Comet
import io.cyborgsquirrel.lighting.enums.Direction
import io.cyborgsquirrel.lighting.enums.FadeCurve
import io.cyborgsquirrel.lighting.model.RgbColor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// TODO palette support
class WaveLightEffect(
    private val numberOfLeds: Int,
    private val settings: WaveEffectSettings,
    private var palette: ColorPalette?,
) :
    LightEffect {

    private var waveALocation = 0
    private var waveBLocation = 0
    private var frame = 0
    private var iterations = 0
    private lateinit var waveA: Comet
    private lateinit var waveB: Comet
    private val waveLength = settings.waveLength
    private val startPoint = settings.startPoint
    private var buffer = listOf<RgbColor>()

    override fun getNextStep(): List<RgbColor> {
        val rgbData = mutableListOf<RgbColor>()
        if (waveALocation <= -waveLength && waveBLocation >= numberOfLeds + waveLength) {
            iterations++
            waveALocation = startPoint - 1
            waveBLocation = startPoint
        } else if (frame == 0) {
            waveALocation = startPoint - 1
            waveBLocation = startPoint
            waveA = Comet(RgbColor.Red, waveLength, FadeCurve.Logarithmic, Direction.HighToLow)
            waveB = Comet(RgbColor.Blue, waveLength, FadeCurve.Logarithmic, Direction.LowToHigh)
        } else {
            if (waveALocation > -waveLength) waveALocation--
            if (waveBLocation < numberOfLeds + waveLength) waveBLocation++
        }

        val waveARgbData = waveA.buffer.subList(
            if (waveALocation > 0) 0 else abs(waveALocation),
            min(waveA.buffer.size, startPoint - waveALocation)
        )
        val waveBRgbData = waveB.buffer.subList(
            max(waveB.buffer.size - 1 - (waveBLocation - startPoint), 0),
            waveB.buffer.size
        )

        for (i in 0..<waveALocation) {
            rgbData.add(RgbColor.Blank)
        }

        rgbData.addAll(waveARgbData)

        for (i in 0..<waveBLocation - rgbData.size - waveBRgbData.size) {
            rgbData.add(RgbColor.Blank)
        }

        rgbData.addAll(waveBRgbData)

        for (i in 0..<numberOfLeds - rgbData.size) {
            rgbData.add(RgbColor.Blank)
        }

        frame++
        buffer = rgbData
        return rgbData
    }

    override fun getBuffer(): List<RgbColor> = buffer

    override fun getSettings() = settings

    override fun getIterations() = iterations

    override fun updatePalette(palette: ColorPalette) {
        this.palette = palette
    }
}