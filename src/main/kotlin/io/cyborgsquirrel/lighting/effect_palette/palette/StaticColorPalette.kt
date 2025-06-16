package io.cyborgsquirrel.lighting.effect_palette.palette

import io.cyborgsquirrel.lighting.effect_palette.settings.StaticPaletteSettings
import io.cyborgsquirrel.lighting.model.LedStrip

class StaticColorPalette(private val settings: StaticPaletteSettings, uuid: String) : ColorPalette(uuid) {
    override fun getPrimaryColor(index: Int, strip: LedStrip) = settings.palette.primaryColor

    override fun getSecondaryColor(index: Int, strip: LedStrip) = settings.palette.secondaryColor

    override fun getTertiaryColor(index: Int, strip: LedStrip) = settings.palette.tertiaryColor

    override fun getOtherColors(index: Int, strip: LedStrip) = settings.palette.otherColors
}