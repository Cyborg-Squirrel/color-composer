package io.cyborgsquirrel.lighting.effect_palette.helper

import io.cyborgsquirrel.lighting.effect_palette.settings.Palette
import java.time.LocalDateTime

class TimePaletteHelper {
    fun getPalette(now: LocalDateTime, points: Map<LocalDateTime, Palette>): Palette {
        var timeKey = points.keys.first()
        for (time in points.keys) {
            if (now > time) {
                timeKey = time
            }
        }

        return points[timeKey]!!
    }
}