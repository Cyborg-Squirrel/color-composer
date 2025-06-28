package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effects.settings.BouncingBallEffectSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.util.time.TimeHelper
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Effect inspired by David Plummer's bouncing ball effect tutorial
 *
 * https://github.com/davepl/DavesGarageLEDSeries/blob/master/LED%20Episode%2011/include/bounce.h
 */
class BouncingBallLightEffect(
    private val numberOfLeds: Int,
    private val timeHelper: TimeHelper,
    private val settings: BouncingBallEffectSettings,
    private var palette: ColorPalette?,
) : LightEffect {
    // Higher values slow the effect
    private val speedKnob = settings.speed

    private var clockTimeAtLastBounce = unixTime()
    private var height = settings.startingHeight
    private var ballSpeed = initialBallSpeed(settings.startingHeight)
    private val dampening = 0.90
    private var iterations = 0
    private lateinit var backupColor: RgbColor
    private var buffer = listOf<RgbColor>()

    override fun getNextStep(): List<RgbColor> {
        val ballLocation = getBallPosition()
        val rgbList = mutableListOf<RgbColor>()
        for (i in 0..<ballLocation) {
            rgbList.add(RgbColor.Blank)
        }

        // Ball length of 2 looks better than 1
        rgbList.add(getColor(ballLocation))
        rgbList.add(getColor(ballLocation + 1))

        for (i in rgbList.size..<numberOfLeds) {
            rgbList.add(RgbColor.Blank)
        }

        buffer = rgbList
        return rgbList
    }

    override fun getBuffer(): List<RgbColor> = buffer

    override fun getSettings() = settings

    override fun getIterations() = iterations

    override fun updatePalette(palette: ColorPalette) {
        this.palette = palette
    }

    private fun initialBallSpeed(height: Double): Double {
        return sqrt(-2 * settings.gravity * height)
    }

    private fun getBallPosition(): Int {
        val timeSinceLastBounce = (unixTime() - clockTimeAtLastBounce) / speedKnob
        height = 0.5 * settings.gravity * timeSinceLastBounce.pow(2.0) + ballSpeed * timeSinceLastBounce

        if (height <= 0.0) {
            height = 0.0
            ballSpeed *= dampening
            clockTimeAtLastBounce = unixTime()
            if (ballSpeed < settings.minimumSpeed) {
                iterations++
                ballSpeed = initialBallSpeed(settings.startingHeight) * dampening
            }
        }

        val position = (height * settings.maxHeight / settings.startingHeight).toInt() % numberOfLeds
        return position
    }

    private fun unixTime(): Double {
        return timeHelper.millisSinceEpoch() / 1000.0
    }

    private fun getColor(index: Int): RgbColor {
        return if (palette != null) {
            palette!!.getPrimaryColor(index)
        } else {
            if (this::backupColor.isInitialized) {
                backupColor
            } else {
                val backupColorSeed = settings.startingHeight.toInt() % 3
                when (backupColorSeed) {
                    2 -> RgbColor.Red
                    1 -> RgbColor.Green
                    else -> RgbColor.Blue
                }
            }
        }
    }
}
