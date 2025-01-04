package io.cyborgsquirrel.sunrise_sunset.time

import jakarta.inject.Singleton
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Singleton
class SunriseSunsetTimeHelperImpl : SunriseSunsetTimeHelper {
    private val formatter = DateTimeFormatter.ISO_DATE_TIME

    override fun today(): LocalDate {
        return LocalDate.now()
    }

    override fun tomorrow(): LocalDate {
        return LocalDate.now().plusDays(1)
    }

    override fun utcTimestampToZoneDateTime(utcDateTimeString: String): ZonedDateTime {
        return ZonedDateTime.parse(utcDateTimeString, formatter).withZoneSameInstant(
            TimeZone.getDefault().toZoneId()
        )
    }
}