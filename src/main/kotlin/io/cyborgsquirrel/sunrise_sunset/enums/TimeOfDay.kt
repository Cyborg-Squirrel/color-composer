package io.cyborgsquirrel.sunrise_sunset.enums

import io.micronaut.serde.annotation.Serdeable

@Serdeable
enum class TimeOfDay {
    Midnight,
    Sunrise,
    Noon,
    Sunset
}