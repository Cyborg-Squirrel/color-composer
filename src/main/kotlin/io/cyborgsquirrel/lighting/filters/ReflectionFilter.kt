package io.cyborgsquirrel.lighting.filters

import io.cyborgsquirrel.lighting.enums.ReflectionType
import io.cyborgsquirrel.lighting.filters.settings.ReflectionFilterSettings
import io.cyborgsquirrel.lighting.model.RgbColor
import io.micronaut.serde.annotation.Serdeable

@Serdeable
class ReflectionFilter(val settings: ReflectionFilterSettings, uuid: String) : LightEffectFilter(uuid) {

    private val reflectionType = settings.reflectionType

    override fun apply(buffer: Array<RgbColor>): Array<RgbColor> {
        // Copy values rather than reassigning array slots: the renderer reuses this buffer across frames, so aliasing
        // two slots to the same RgbColor would let a later in-place write corrupt its mirror pixel.
        if (reflectionType == ReflectionType.CopyOverCenter) {
            for (i in buffer.indices) {
                if (buffer[i].isBlank()) {
                    buffer[i].copyFrom(buffer[buffer.size - i - 1])
                }
            }
        } else {
            val isLowToHigh = reflectionType == ReflectionType.LowToHigh
            for (i in buffer.indices) {
                if (i < buffer.size / 2 && !isLowToHigh) buffer[i].copyFrom(buffer[buffer.size - 1 - i])
                if (i >= buffer.size / 2 && isLowToHigh) buffer[i].copyFrom(buffer[buffer.size - 1 - i])
            }
        }

        return buffer
    }
}
