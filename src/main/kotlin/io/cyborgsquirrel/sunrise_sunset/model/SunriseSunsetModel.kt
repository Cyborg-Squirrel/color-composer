package io.cyborgsquirrel.sunrise_sunset.model

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class SunriseSunsetModel(val results: SunriseSunsetResult, val status: String, val tzid: String)