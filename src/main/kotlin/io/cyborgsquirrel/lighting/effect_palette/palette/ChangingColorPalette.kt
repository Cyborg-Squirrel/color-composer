package io.cyborgsquirrel.lighting.effect_palette.palette

import io.cyborgsquirrel.lighting.effect_palette.enums.ChangingPaletteStep
import io.cyborgsquirrel.lighting.effect_palette.helper.GradientColorHelper
import io.cyborgsquirrel.lighting.effect_palette.settings.ChangingGradientPaletteSettings
import io.cyborgsquirrel.lighting.effect_palette.settings.ChangingPaletteSettings
import io.cyborgsquirrel.lighting.effect_palette.settings.ChangingStaticPaletteSettings
import io.cyborgsquirrel.lighting.effect_palette.settings.SettingsPalette
import io.cyborgsquirrel.lighting.model.LedStrip
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.util.time.TimeHelper

class ChangingColorPalette(
    private val settings: ChangingPaletteSettings,
    private val timeHelper: TimeHelper,
    uuid: String,
    strip: LedStrip,
) :
    ColorPalette(uuid, strip) {

    private var counter = 0
    private var holdStart = 0L
    private var transitionStart = 0L
    private val helper = GradientColorHelper()
    private var step = ChangingPaletteStep.ColorHold

    override fun getPrimaryColor(index: Int): RgbColor {
        val currentPalette = getCurrentPalette(index)
        handleState()
        return when (step) {
            ChangingPaletteStep.StartColorHold, ChangingPaletteStep.ColorHold, ChangingPaletteStep.StartTransition -> {
                currentPalette.primaryColor
            }

            ChangingPaletteStep.Transition -> transition(
                currentPalette.primaryColor,
                getNextPalette(index).primaryColor
            )
        }
    }

    override fun getSecondaryColor(index: Int): RgbColor {
        val currentPalette = getCurrentPalette(index)
        handleState()
        return when (step) {
            ChangingPaletteStep.StartColorHold, ChangingPaletteStep.ColorHold, ChangingPaletteStep.StartTransition -> {
                currentPalette.secondaryColor
            }

            ChangingPaletteStep.Transition -> transition(
                currentPalette.secondaryColor,
                getNextPalette(index).secondaryColor
            )
        }
    }

    override fun getTertiaryColor(index: Int): RgbColor? {
        val currentPalette = getCurrentPalette(index)
        val nextPalette = if (step == ChangingPaletteStep.Transition) getNextPalette(index) else null
        handleState()
        return if (currentPalette.tertiaryColor != null && nextPalette?.tertiaryColor != null) {
            when (step) {
                ChangingPaletteStep.StartColorHold, ChangingPaletteStep.ColorHold, ChangingPaletteStep.StartTransition -> {
                    currentPalette.tertiaryColor
                }

                ChangingPaletteStep.Transition -> transition(
                    currentPalette.tertiaryColor,
                    nextPalette.tertiaryColor
                )
            }
        } else {
            null
        }
    }

    override fun getOtherColors(index: Int): List<RgbColor> {
        val currentPalette = getCurrentPalette(index)
        val nextPaletteOtherColors =
            if (step == ChangingPaletteStep.Transition) getNextPalette(index).otherColors else listOf()
        handleState()
        return currentPalette.otherColors.zip(nextPaletteOtherColors).map {
            when (step) {
                ChangingPaletteStep.StartColorHold, ChangingPaletteStep.ColorHold, ChangingPaletteStep.StartTransition -> {
                    it.first
                }

                ChangingPaletteStep.Transition -> transition(
                    it.first,
                    it.second
                )
            }
        }
    }

    private fun getCurrentPalette(index: Int): SettingsPalette {
        return when (settings) {
            is ChangingStaticPaletteSettings -> settings.palettes[counter % settings.palettes.size]
            is ChangingGradientPaletteSettings -> helper.getPalette(
                index,
                strip,
                settings.gradientList[counter % settings.gradientList.size]
            )
        }
    }

    private fun getNextPalette(index: Int): SettingsPalette {
        return when (settings) {
            is ChangingStaticPaletteSettings -> settings.palettes[(counter + 1) % settings.palettes.size]
            is ChangingGradientPaletteSettings -> helper.getPalette(
                index,
                strip,
                settings.gradientList[(counter + 1) % settings.gradientList.size]
            )
        }
    }

    private fun handleState() {
        val now = timeHelper.millisSinceEpoch()
        when (step) {
            ChangingPaletteStep.StartColorHold -> {
                step = ChangingPaletteStep.ColorHold
                holdStart = now
            }

            ChangingPaletteStep.ColorHold -> {
                if (now - holdStart >= settings.paletteHoldTime().toMillis()) {
                    // Begin transition to next color
                    step = ChangingPaletteStep.StartTransition
                    transitionStart = 0L
                }
            }

            ChangingPaletteStep.StartTransition -> {
                step = ChangingPaletteStep.Transition
                transitionStart = now
            }

            ChangingPaletteStep.Transition -> {}
        }
    }

    private fun transition(currentColor: RgbColor, nextColor: RgbColor): RgbColor {
        val now = timeHelper.millisSinceEpoch()
        return if (now - transitionStart > settings.paletteTransitionTime().toMillis()) {
            // Transition complete, return next color and hold
            step = ChangingPaletteStep.StartColorHold
            holdStart = 0L
            counter++
            nextColor
        } else {
            // Blend between current and next palette
            val fractionComplete =
                (now - transitionStart).toFloat() / settings.paletteTransitionTime().toMillis()
            currentColor.interpolate(nextColor, fractionComplete)
        }
    }
}