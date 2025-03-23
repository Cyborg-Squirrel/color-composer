package io.cyborgsquirrel.util.time

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

/**
 * Interface for getting the current time. Provides a layer between service classes and LocalDate/LocalDateTime for
 * easier mocking and testing.
 */
interface TimeHelper {
    fun today(): LocalDate

    fun tomorrow(): LocalDate

    fun now(): LocalDateTime

    fun millisSinceEpoch(): Long

    fun dateTimeFromMillis(millisSinceEpoch: Long): LocalDateTime

    fun utcTimestampToZoneDateTime(utcDateTimeString: String): ZonedDateTime
}