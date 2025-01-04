package io.cyborgsquirrel.sunrise_sunset.time

import java.time.LocalDate
import java.time.ZonedDateTime

interface SunriseSunsetTimeHelper {
    fun today(): LocalDate

    fun tomorrow(): LocalDate

    fun utcTimestampToZoneDateTime(utcDateTimeString: String): ZonedDateTime
}