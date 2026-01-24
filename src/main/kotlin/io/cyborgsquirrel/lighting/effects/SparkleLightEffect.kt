package io.cyborgsquirrel.lighting.effects

import io.cyborgsquirrel.lighting.effect_palette.palette.ColorPalette
import io.cyborgsquirrel.lighting.effects.settings.SparkleEffectSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.util.time.TimeHelper
import kotlin.random.Random

/**
 * Sparkle effect with random glowing dots that fade in and out.
 * Each dot appears at a random LED position, fades in quickly to full intensity
 * and then fades out. The fade rates and maximum simultaneous dots are configurable.
 */
class SparkleLightEffect(
    private val numberOfLeds: Int,
    override val settings: SparkleEffectSettings,
    override var palette: ColorPalette?,
    private val timeHelper: TimeHelper,
) : LightEffect(settings, palette) {

    private data class Dot(
        val position: Int,
        var intensity: Float,
        val startTime: Long,
        val fadeInMillis: Int,
        val fadeOutMillis: Int
    )

    private val dots = mutableListOf<Dot>()
    private var buffer = MutableList(numberOfLeds) { RgbColor.Blank }
    private var iterations = 0

    override fun getNextStep(): List<RgbColor> {
        val now = timeHelper.millisSinceEpoch()

        val iterator = dots.iterator()
        while (iterator.hasNext()) {
            val dot = iterator.next()
            with(dot) {
                val elapsed = now - startTime
                if (elapsed > fadeInMillis) {
                    val fadeOutPercentDone = elapsed / (fadeInMillis + fadeOutMillis).toFloat()
                    if (fadeOutPercentDone >= 0.98) {
                        iterator.remove()
                    } else {
                        intensity = 1 - fadeOutPercentDone
                    }
                } else {
                    val fadeInPercentDone = elapsed / fadeInMillis.toFloat()
                    intensity = fadeInPercentDone
                }
            }
        }

        while (dots.size < settings.numDots) {
            val position = Random.nextInt(numberOfLeds)
            val fadeInMillis = Random.nextInt(settings.fadeInMillisMin, settings.fadeInMillisMax)
            val fadeOutMillis = Random.nextInt(settings.fadeOutMillisMin, settings.fadeOutMillisMax)
            dots.add(Dot(position, 0f,now, fadeInMillis, fadeOutMillis))
        }

        buffer = MutableList(numberOfLeds) { RgbColor.Blank }
        dots.forEach { dot ->
            buffer[dot.position] = getColor(dot).scale(dot.intensity)
        }

        iterations++
        return buffer
    }

    override fun getBuffer(): List<RgbColor> = buffer

    override fun getIterations(): Int = iterations

    override fun updatePalette(palette: ColorPalette) {
        this.palette = palette
    }

    private fun getColor(dot: Dot): RgbColor {
        return if (dot.startTime % 3 == 0L) {
            palette?.getSecondaryColor(dot.position) ?: RgbColor.Purple
        } else if (dot.startTime % 4 == 0L) {
            palette?.getTertiaryColor(dot.position) ?: RgbColor.Cyan
        } else {
            palette?.getPrimaryColor(dot.position) ?: RgbColor.Green
        }
    }
}
