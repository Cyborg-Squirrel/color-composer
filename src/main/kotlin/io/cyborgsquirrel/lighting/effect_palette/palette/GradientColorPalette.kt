package io.cyborgsquirrel.lighting.effect_palette.palette

import io.cyborgsquirrel.lighting.effect_palette.helper.GradientColorHelper
import io.cyborgsquirrel.lighting.effect_palette.settings.GradientPaletteSettings
import io.cyborgsquirrel.lighting.model.LedStrip

class GradientColorPalette(private val settings: GradientPaletteSettings, uuid: String) : ColorPalette(uuid) {

    private val helper = GradientColorHelper()

    override fun getPrimaryColor(index: Int, strip: LedStrip) =
        helper.getPalette(index, strip, settings.points).primaryColor

    override fun getSecondaryColor(index: Int, strip: LedStrip) =
        helper.getPalette(index, strip, settings.points).secondaryColor

    override fun getTertiaryColor(index: Int, strip: LedStrip) =
        helper.getPalette(index, strip, settings.points).tertiaryColor

    override fun getOtherColors(index: Int, strip: LedStrip) =
        helper.getPalette(index, strip, settings.points).otherColors
}