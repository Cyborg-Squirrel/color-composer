package io.cyborgsquirrel.lighting.effect_palette.palette

import io.cyborgsquirrel.lighting.effect_palette.enums.ChangingPaletteStep
import io.cyborgsquirrel.lighting.effect_palette.helper.GradientColorHelper
import io.cyborgsquirrel.lighting.effect_palette.settings.ChangingGradientPaletteSettings
import io.cyborgsquirrel.lighting.effect_palette.settings.ChangingPaletteSettings
import io.cyborgsquirrel.lighting.effect_palette.settings.ChangingStaticPaletteSettings
import io.cyborgsquirrel.lighting.effect_palette.settings.Palette
import io.cyborgsquirrel.lighting.model.LedStrip
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.util.time.TimeHelper

class ChangingColorPalette(
    private val settings: ChangingPaletteSettings,
    private val timeHelper: TimeHelper,
    uuid: String
) :
    ColorPalette(uuid) {

    private var counter = 0
    private var holdStart = 0L
    private var transitionStart = 0L
    private val helper = GradientColorHelper()
    private var step = ChangingPaletteStep.ColorHold

    private fun getColor(currentColor: RgbColor, nextColor: RgbColor): RgbColor {
        val now = timeHelper.millisSinceEpoch()
        return when (step) {
            ChangingPaletteStep.StartColorHold -> {
                step = ChangingPaletteStep.ColorHold
                holdStart = now
                currentColor
            }

            ChangingPaletteStep.ColorHold -> {
                if (now - holdStart >= settings.paletteHoldTime().inWholeMilliseconds) {
                    // Begin transition to next color
                    step = ChangingPaletteStep.StartTransition
                    transitionStart = 0L
                }

                currentColor
            }

            ChangingPaletteStep.StartTransition -> {
                step = ChangingPaletteStep.Transition
                transitionStart = now
                currentColor
            }

            ChangingPaletteStep.Transition -> {
                if (now - transitionStart > settings.paletteTransitionTime().inWholeMilliseconds) {
                    // Transition complete, return next color and hold
                    step = ChangingPaletteStep.StartColorHold
                    holdStart = 0L
                    counter++
                    nextColor
                } else {
                    // Blend between current and next palette
                    val fractionComplete =
                        (now - transitionStart).toFloat() / settings.paletteTransitionTime().inWholeMilliseconds
                    currentColor.interpolate(nextColor, fractionComplete)
                }
            }
        }
    }

    private fun getCurrentPalette(index: Int, strip: LedStrip): Palette {
        return when (settings) {
            is ChangingStaticPaletteSettings -> settings.palettes[counter % settings.palettes.size]
            is ChangingGradientPaletteSettings -> helper.getPalette(
                index,
                strip,
                settings.gradientList[counter % settings.gradientList.size]
            )
        }
    }

    private fun getNextPalette(index: Int, strip: LedStrip): Palette {
        return when (settings) {
            is ChangingStaticPaletteSettings -> settings.palettes[counter + 1 % settings.palettes.size]
            is ChangingGradientPaletteSettings -> helper.getPalette(
                index,
                strip,
                settings.gradientList[counter + 1 % settings.gradientList.size]
            )
        }
    }

    override fun getPrimaryColor(index: Int, strip: LedStrip): RgbColor {
        val currentPalette = getCurrentPalette(index, strip)
        val nextPalette = getNextPalette(index, strip)
        return getColor(currentPalette.primaryColor, nextPalette.primaryColor)
    }

    override fun getSecondaryColor(index: Int, strip: LedStrip): RgbColor {
        val currentPalette = getCurrentPalette(index, strip)
        val nextPalette = getNextPalette(index, strip)
        return getColor(currentPalette.secondaryColor, nextPalette.secondaryColor)
    }

    override fun getTertiaryColor(index: Int, strip: LedStrip): RgbColor? {
        val currentPalette = getCurrentPalette(index, strip)
        val nextPalette = getNextPalette(index, strip)
        return if (currentPalette.tertiaryColor != null && nextPalette.tertiaryColor != null) {
            getColor(currentPalette.tertiaryColor, nextPalette.tertiaryColor)
        } else {
            null
        }
    }

    override fun getOtherColors(index: Int, strip: LedStrip): List<RgbColor> {
        val currentPalette = getCurrentPalette(index, strip)
        val nextPalette = getNextPalette(index, strip)
        return currentPalette.otherColors.zip(nextPalette.otherColors).map {
            getColor(it.first, it.second)
        }
    }
}