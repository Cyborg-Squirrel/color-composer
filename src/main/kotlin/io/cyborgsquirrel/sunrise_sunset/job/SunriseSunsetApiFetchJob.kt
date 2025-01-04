package io.cyborgsquirrel.sunrise_sunset.job

import io.cyborgsquirrel.entity.LocationConfigEntity
import io.cyborgsquirrel.entity.SunriseSunsetTimeEntity
import io.cyborgsquirrel.repository.H2LocationConfigRepository
import io.cyborgsquirrel.repository.H2SunriseSunsetTimeRepository
import io.cyborgsquirrel.sunrise_sunset.client.SunriseSunsetApiClient
import io.cyborgsquirrel.sunrise_sunset.time.SunriseSunsetTimeHelper
import io.micronaut.serde.ObjectMapper
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@Singleton
class SunriseSunsetApiFetchJob(
    private val api: SunriseSunsetApiClient,
    private val locationRepository: H2LocationConfigRepository,
    private val sunriseSunsetRepository: H2SunriseSunsetTimeRepository,
    private val objectMapper: ObjectMapper,
    private val sunriseSunsetTimeHelper: SunriseSunsetTimeHelper,
) : Runnable {

    override fun run() {
        logger.info("Running sunrise sunset time api client fetch")
        val activeLocation = locationRepository.findByActiveTrue()

        if (activeLocation.isPresent) {
            val location = activeLocation.get()
            val today = sunriseSunsetTimeHelper.today()
            val tomorrow = sunriseSunsetTimeHelper.tomorrow()
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
                val sunriseTime = sunriseSunsetTimeHelper.utcTimestampToZoneDateTime(sunriseSunsetModel.results.sunrise)
                val sunsetTime = sunriseSunsetTimeHelper.utcTimestampToZoneDateTime(sunriseSunsetModel.results.sunset)

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
            } else {
                logger.info("$ymdString has already been fetched for the configured location. Skipping api fetch.")
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