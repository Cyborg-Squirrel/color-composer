package io.cyborgsquirrel.lighting.effect_palette.palette

import io.cyborgsquirrel.lighting.effect_palette.helper.GradientColorHelper
import io.cyborgsquirrel.lighting.effect_palette.settings.GradientPaletteSettings
import io.cyborgsquirrel.lighting.effect_palette.settings.SettingsPalette
import io.cyborgsquirrel.lighting.model.LedStrip
import io.cyborgsquirrel.lighting.model.RgbColor

class GradientColorPalette(private val settings: GradientPaletteSettings, uuid: String, strip: LedStrip) :
    ColorPalette(uuid, strip) {

    private val helper = GradientColorHelper()
    private val cache = mutableMapOf<Int, SettingsPalette>()

    override fun getPrimaryColor(index: Int): RgbColor {
        val cachedColor = cache[index]
        if (cachedColor != null) {
            return cachedColor.primaryColor
        } else {
            val palette = helper.getPalette(index, strip, settings.points)
            cache[index] = palette
            return palette.primaryColor
        }
    }

    override fun getSecondaryColor(index: Int): RgbColor {
        val cachedColor = cache[index]
        if (cachedColor != null) {
            return cachedColor.secondaryColor
        } else {
            val palette = helper.getPalette(index, strip, settings.points)
            cache[index] = palette
            return palette.secondaryColor
        }
    }

    override fun getTertiaryColor(index: Int): RgbColor? {
        val cachedColor = cache[index]
        if (cachedColor != null) {
            return cachedColor.tertiaryColor
        } else {
            val palette = helper.getPalette(index, strip, settings.points)
            cache[index] = palette
            return palette.tertiaryColor
        }
    }

    override fun getOtherColors(index: Int): List<RgbColor> {
        val cachedColor = cache[index]
        if (cachedColor != null) {
            return cachedColor.otherColors
        } else {
            val palette = helper.getPalette(index, strip, settings.points)
            cache[index] = palette
            return palette.otherColors
        }
    }
}