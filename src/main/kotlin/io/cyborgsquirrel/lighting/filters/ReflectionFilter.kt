package io.cyborgsquirrel.lighting.filters

import io.cyborgsquirrel.lighting.enums.ReflectionType
import io.cyborgsquirrel.lighting.filters.settings.ReflectionFilterSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import io.micronaut.serde.annotation.Serdeable

@Serdeable
class ReflectionFilter(val settings: ReflectionFilterSettings, uuid: String) : LightEffectFilter(uuid) {

    private val reflectionType = settings.reflectionType

    override fun apply(buffer: Array<RgbColor>): Array<RgbColor> {
        if (reflectionType == ReflectionType.CopyOverCenter) {
            for (i in buffer.indices) {
                if (buffer[i].isBlank()) {
                    buffer[i] = buffer[buffer.size - i - 1]
                }
            }
        } else {
            val isLowToHigh = reflectionType == ReflectionType.LowToHigh
            for (i in buffer.indices) {
                if (i < buffer.size / 2 && !isLowToHigh) buffer[i] = buffer[buffer.size - 1 - i]
                if (i >= buffer.size / 2 && isLowToHigh) buffer[i] = buffer[buffer.size - 1 - i]
            }
        }

        return buffer
    }
}
