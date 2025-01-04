package io.cyborgsquirrel.job

import io.cyborgsquirrel.client.SunriseSunsetApiClient
import io.cyborgsquirrel.entity.LocationConfigEntity
import io.cyborgsquirrel.entity.SunriseSunsetTimeEntity
import io.cyborgsquirrel.repository.H2LocationConfigRepository
import io.cyborgsquirrel.repository.H2SunriseSunsetTimeRepository
import io.micronaut.serde.ObjectMapper
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

@Singleton
class SunriseSunsetApiFetchJob(
    private val api: SunriseSunsetApiClient,
    private val locationRepository: H2LocationConfigRepository,
    private val sunriseSunsetRepository: H2SunriseSunsetTimeRepository,
    private val objectMapper: ObjectMapper,
) : Runnable {
    private val formatter = DateTimeFormatter.ISO_DATE_TIME

    override fun run() {
        logger.info("Running sunrise sunset time api client fetch")
        val activeLocation = locationRepository.findByActiveTrue()

        if (activeLocation.isPresent) {
            val location = activeLocation.get()
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            fetchSunriseSunsetTimesForDate(location, today)
            fetchSunriseSunsetTimesForDate(location, tomorrow)
        }
    }

    private fun fetchSunriseSunsetTimesForDate(location: LocationConfigEntity, date: LocalDate) {
        try {
            val ymdString = getYmdString(date)
            val lat = location.latitude!!
            val long = location.longitude!!
            val sunriseSunsetTime = sunriseSunsetRepository.findByYmdEqualsAndLocationEquals(ymdString, location)

            if (sunriseSunsetTime.isEmpty) {
                val sunriseSunsetModel =
                    api.getSunriseSunsetTimes(lat, long, ymdString).get(2, TimeUnit.SECONDS)
                val sunriseTime =
                    ZonedDateTime.parse(sunriseSunsetModel.results.sunrise, formatter).withZoneSameInstant(
                        TimeZone.getDefault().toZoneId()
                    )
                val sunsetTime = ZonedDateTime.parse(sunriseSunsetModel.results.sunset, formatter).withZoneSameInstant(
                    TimeZone.getDefault().toZoneId()
                )

                logger.info("Received sunrise/sunset times for $ymdString (ymd)")
                logger.info("Location: $lat $long | Sunrise: ${sunriseTime.toLocalTime()} | Sunset: ${sunsetTime.toLocalTime()}")

                val jsonString = objectMapper.writeValueAsString(sunriseSunsetModel)
                sunriseSunsetRepository.save(
                    SunriseSunsetTimeEntity(
                        ymd = ymdString,
                        json = jsonString,
                        location = location
                    )
                )
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            logger.error(ex.toString())
        }
    }

    private fun getYmdString(date: LocalDate): String {
        val year = date.year
        val month = if (date.monthValue < 10) "0${date.monthValue}" else date.monthValue.toString()
        val day = if (date.dayOfMonth < 10) "0${date.dayOfMonth}" else date.dayOfMonth
        return "$year-$month-$day"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SunriseSunsetApiFetchJob::class.java)
    }
}