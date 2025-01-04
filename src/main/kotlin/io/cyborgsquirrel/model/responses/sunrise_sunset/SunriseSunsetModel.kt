package io.cyborgsquirrel.model.responses.sunrise_sunset

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class SunriseSunsetModel(val results: SunriseSunsetResult, val status: String, val tzid: String)