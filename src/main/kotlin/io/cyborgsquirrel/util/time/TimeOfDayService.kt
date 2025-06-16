package io.cyborgsquirrel.util.time

import io.cyborgsquirrel.sunrise_sunset.enums.TimeOfDay
import io.cyborgsquirrel.sunrise_sunset.model.SunriseSunsetModel
import java.time.LocalDateTime
import java.time.LocalTime

interface TimeOfDayService {
    fun timeOfDayToLocalDateTime(todaySunriseSunsetData: SunriseSunsetModel, timeOfDay: TimeOfDay): LocalDateTime

    fun defaultTimeOf(timeOfDay: TimeOfDay): LocalTime
}