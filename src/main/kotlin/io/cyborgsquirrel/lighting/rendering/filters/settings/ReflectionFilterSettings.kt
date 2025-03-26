package io.cyborgsquirrel.lighting.rendering.filters.settings

import io.cyborgsquirrel.lighting.enums.ReflectionType
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ReflectionFilterSettings(val reflectionType: ReflectionType)