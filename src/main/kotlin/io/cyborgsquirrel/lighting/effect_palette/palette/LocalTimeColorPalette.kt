package io.cyborgsquirrel.lighting.effect_palette.palette

import io.cyborgsquirrel.lighting.effect_palette.helper.TimePaletteHelper
import io.cyborgsquirrel.lighting.effect_palette.settings.LocalTimePaletteSettings
import io.cyborgsquirrel.lighting.effect_palette.settings.Palette
import io.cyborgsquirrel.lighting.model.LedStrip
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.util.time.TimeHelper
import java.time.LocalDateTime

class LocalTimeColorPalette(
    private val settings: LocalTimePaletteSettings, private val timeHelper: TimeHelper, uuid: String
) : ColorPalette(uuid) {

    private val helper = TimePaletteHelper()

    override fun getPrimaryColor(index: Int, strip: LedStrip): RgbColor {
        val now = timeHelper.now()
        val points = getPalettePoints(now)
        val palette = helper.getPalette(now, points)
        return palette.primaryColor
    }

    override fun getSecondaryColor(index: Int, strip: LedStrip): RgbColor {
        val now = timeHelper.now()
        val points = getPalettePoints(now)
        val palette = helper.getPalette(now, points)
        return palette.secondaryColor
    }

    override fun getTertiaryColor(index: Int, strip: LedStrip): RgbColor? {
        val now = timeHelper.now()
        val points = getPalettePoints(now)
        val palette = helper.getPalette(now, points)
        return palette.tertiaryColor
    }

    override fun getOtherColors(index: Int, strip: LedStrip): List<RgbColor> {
        val now = timeHelper.now()
        val points = getPalettePoints(now)
        val palette = helper.getPalette(now, points)
        return palette.otherColors
    }

    private fun getPalettePoints(now: LocalDateTime): Map<LocalDateTime, Palette> {
        return settings.paletteMap.mapKeys {
            now.withHour(it.key.hour).withMinute(it.key.minute)
        }
    }
}