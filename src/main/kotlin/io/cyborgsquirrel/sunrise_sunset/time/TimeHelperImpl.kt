package io.cyborgsquirrel.sunrise_sunset.time

import jakarta.inject.Singleton
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Singleton
class TimeHelperImpl : TimeHelper {
    private val formatter = DateTimeFormatter.ISO_DATE_TIME

    override fun today(): LocalDate {
        return LocalDate.now()
    }

    override fun tomorrow(): LocalDate {
        return LocalDate.now().plusDays(1)
    }

    override fun now(): LocalDateTime {
        return LocalDateTime.now()
    }

    override fun utcTimestampToZoneDateTime(utcDateTimeString: String): ZonedDateTime {
        return ZonedDateTime.parse(utcDateTimeString, formatter).withZoneSameInstant(
            TimeZone.getDefault().toZoneId()
        )
    }
}