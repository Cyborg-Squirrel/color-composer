package io.cyborgsquirrel.lighting.enums

import io.micronaut.serde.annotation.Serdeable

@Serdeable
enum class FadeCurve {
    Linear,
    Logarithmic
}