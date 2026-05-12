package io.cyborgsquirrel.lighting.effect_palette.palette

import io.cyborgsquirrel.lighting.effect_palette.settings.StaticPaletteHasMetadata

class StaticColorPalette(private val settings: StaticPaletteHasMetadata, uuid: String, numberOfLeds: Int) :
    ColorPalette(uuid, numberOfLeds) {
    override fun getPrimaryColor(index: Int) = settings.palette.primaryColor

    override fun getSecondaryColor(index: Int) = settings.palette.secondaryColor

    override fun getTertiaryColor(index: Int) = settings.palette.tertiaryColor

    override fun getOtherColors(index: Int) = settings.palette.otherColors
}