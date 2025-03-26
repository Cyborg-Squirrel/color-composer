package io.cyborgsquirrel.lighting.rendering.filters

import io.cyborgsquirrel.lighting.enums.ReflectionType
import io.cyborgsquirrel.lighting.rendering.filters.settings.ReflectionFilterSettings
import io.cyborgsquirrel.model.color.RgbColor
import io.micronaut.serde.annotation.Serdeable

@Serdeable
class ReflectionFilter(val settings: ReflectionFilterSettings, uuid: String) : LightEffectFilter(uuid) {

    private val reflectionType = settings.reflectionType

    override fun apply(rgbList: List<RgbColor>): List<RgbColor> {
        val reflectedList = mutableListOf<RgbColor>()

        if (reflectionType == ReflectionType.CopyOverCenter) {
            for (i in rgbList.indices) {
                if (rgbList[i].isBlank()) {
                    reflectedList.add(rgbList[rgbList.size - 1 - i])
                } else {
                    reflectedList.add(rgbList[i])
                }
            }
        } else {
            val isLowToHigh = reflectionType == ReflectionType.LowToHigh

            for (i in rgbList.indices) {
                if (i < rgbList.size / 2) {
                    if (isLowToHigh) reflectedList.add(rgbList[i]) else reflectedList.add(rgbList[rgbList.size - 1 - i])
                } else {
                    if (isLowToHigh) reflectedList.add(rgbList[rgbList.size - 1 - i]) else reflectedList.add(rgbList[i])
                }
            }
        }

        return reflectedList
    }
}