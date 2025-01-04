package io.cyborgsquirrel.model.responses.sunrise_sunset

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.serde.annotation.Serdeable

@Serdeable
class SunriseSunsetResult(
    val sunrise: String,
    val sunset: String,
    @JsonProperty("solar_noon") val solarNoon: String,
    @JsonProperty("day_length") val dayLength: Int,
    @JsonProperty("civil_twilight_begin") val civilTwilightBegin: String,
    @JsonProperty("civil_twilight_end") val civilTwilightEnd: String,
    @JsonProperty("nautical_twilight_begin") val nauticalTwilightBegin: String,
    @JsonProperty("nautical_twilight_end") val nauticalTwilightEnd: String,
    @JsonProperty("astronomical_twilight_begin") val astronomicalTwilightBegin: String,
    @JsonProperty("astronomical_twilight_end") val astronomicalTwilightEnd: String,
)