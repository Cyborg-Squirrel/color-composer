package io.cyborgsquirrel.sunrise_sunset.job

import io.cyborgsquirrel.sunrise_sunset.entity.LocationConfigEntity
import io.cyborgsquirrel.sunrise_sunset.entity.SunriseSunsetTimeEntity
import io.cyborgsquirrel.sunrise_sunset.client.SunriseSunsetApiClient
import io.cyborgsquirrel.sunrise_sunset.repository.H2LocationConfigRepository
import io.cyborgsquirrel.sunrise_sunset.repository.H2SunriseSunsetTimeRepository
import io.cyborgsquirrel.util.time.TimeHelper
import io.cyborgsquirrel.util.time.ymd
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
    private val timeHelper: TimeHelper,
) : Runnable {

    override fun run() {
        logger.info("Running sunrise sunset time api client fetch")
        val activeLocation = locationRepository.findByActiveTrue()

        if (activeLocation.isPresent) {
            val location = activeLocation.get()
            val today = timeHelper.today()
            val tomorrow = timeHelper.tomorrow()
            fetchSunriseSunsetTimesForDate(location, today)
            fetchSunriseSunsetTimesForDate(location, tomorrow)
        }
    }

    private fun fetchSunriseSunsetTimesForDate(location: LocationConfigEntity, date: LocalDate) {
        try {
            val ymdString = date.ymd()
            val lat = location.latitude!!
            val long = location.longitude!!
            val sunriseSunsetTime = sunriseSunsetRepository.findByYmdEqualsAndLocationEquals(ymdString, location)

            if (sunriseSunsetTime.isEmpty) {
                val sunriseSunsetModel =
                    api.getSunriseSunsetTimes(lat, long, ymdString).get(10, TimeUnit.SECONDS)
                val sunriseTime = timeHelper.utcTimestampToZoneDateTime(sunriseSunsetModel.results.sunrise)
                val sunsetTime = timeHelper.utcTimestampToZoneDateTime(sunriseSunsetModel.results.sunset)

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
                logger.info("$ymdString (ymd) has already been saved for the configured location. Skipping api fetch.")
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            logger.error(ex.message)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SunriseSunsetApiFetchJob::class.java)
    }
}