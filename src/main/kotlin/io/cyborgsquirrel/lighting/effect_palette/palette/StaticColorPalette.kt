package io.cyborgsquirrel.lighting.effect_palette.palette

import io.cyborgsquirrel.lighting.effect_palette.settings.StaticPaletteSettings
import io.cyborgsquirrel.lighting.model.LedStripModel

class StaticColorPalette(private val settings: StaticPaletteSettings, uuid: String, strip: LedStripModel) :
    ColorPalette(uuid, strip) {
    override fun getPrimaryColor(index: Int) = settings.palette.primaryColor

    override fun getSecondaryColor(index: Int) = settings.palette.secondaryColor

    override fun getTertiaryColor(index: Int) = settings.palette.tertiaryColor

    override fun getOtherColors(index: Int) = settings.palette.otherColors
}