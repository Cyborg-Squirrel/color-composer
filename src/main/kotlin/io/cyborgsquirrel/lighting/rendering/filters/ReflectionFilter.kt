package io.cyborgsquirrel.lighting.rendering.filters

import io.cyborgsquirrel.lighting.enums.ReflectionType
import io.cyborgsquirrel.model.color.RgbColor

class ReflectionFilter(val reflectionType: ReflectionType) : LightEffectFilter {

    override fun apply(rgbList: List<RgbColor>): List<RgbColor> {
        val reflectedList = mutableListOf<RgbColor>()
        val isLowToHigh = reflectionType == ReflectionType.LowToHigh

        for (i in rgbList.indices) {
            if (i < rgbList.size / 2) {
                if (isLowToHigh) reflectedList.add(rgbList[i]) else reflectedList.add(rgbList[rgbList.size - 1 - i])
            } else {
                if (isLowToHigh) reflectedList.add(rgbList[rgbList.size - 1 - i]) else reflectedList.add(rgbList[i])
            }
        }

        return reflectedList
    }
}