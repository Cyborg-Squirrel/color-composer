package io.cyborgsquirrel.sunrise_sunset.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.serde.annotation.Serdeable

@Serdeable
class SunriseSunsetResult(
    val sunrise: String,
    val sunset: String,
    @param:JsonProperty("solar_noon") val solarNoon: String,
    @param:JsonProperty("day_length") val dayLength: Int,
    @param:JsonProperty("civil_twilight_begin") val civilTwilightBegin: String,
    @param:JsonProperty("civil_twilight_end") val civilTwilightEnd: String,
    @param:JsonProperty("nautical_twilight_begin") val nauticalTwilightBegin: String,
    @param:JsonProperty("nautical_twilight_end") val nauticalTwilightEnd: String,
    @param:JsonProperty("astronomical_twilight_begin") val astronomicalTwilightBegin: String,
    @param:JsonProperty("astronomical_twilight_end") val astronomicalTwilightEnd: String,
)