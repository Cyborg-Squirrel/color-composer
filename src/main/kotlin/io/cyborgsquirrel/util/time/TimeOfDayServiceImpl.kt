package io.cyborgsquirrel.util.time

import io.cyborgsquirrel.sunrise_sunset.enums.TimeOfDay
import io.cyborgsquirrel.sunrise_sunset.model.SunriseSunsetModel
import jakarta.inject.Singleton
import java.time.LocalDateTime
import java.time.LocalTime

@Singleton
class TimeOfDayServiceImpl(private val timeHelper: TimeHelper) : TimeOfDayService {

    override fun timeOfDayToLocalDateTime(
        todaySunriseSunsetData: SunriseSunsetModel,
        timeOfDay: TimeOfDay
    ): LocalDateTime {
        return when (timeOfDay) {
            TimeOfDay.Sunrise -> timeHelper.utcTimestampToZoneDateTime(todaySunriseSunsetData.results.sunrise)
                .toLocalDateTime()

            TimeOfDay.Sunset -> timeHelper.utcTimestampToZoneDateTime(todaySunriseSunsetData.results.sunset)
                .toLocalDateTime()

            TimeOfDay.Noon, TimeOfDay.Midnight -> {
                val now = timeHelper.now()
                val defaultTime = defaultTimeOf(timeOfDay)
                now.withHour(defaultTime.hour).withMinute(defaultTime.minute)
            }
        }
    }

    override fun defaultTimeOf(timeOfDay: TimeOfDay): LocalTime {
        return when (timeOfDay) {
            TimeOfDay.Midnight -> LocalTime.of(0, 0)
            TimeOfDay.Sunrise -> LocalTime.of(7, 0)
            TimeOfDay.Noon -> LocalTime.of(12, 0)
            TimeOfDay.Sunset -> LocalTime.of(7, 0)
        }
    }
}