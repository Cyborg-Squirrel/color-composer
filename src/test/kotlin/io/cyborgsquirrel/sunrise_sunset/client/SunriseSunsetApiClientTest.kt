package io.cyborgsquirrel.sunrise_sunset.client

import io.cyborgsquirrel.util.time.TimeHelper
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


@Ignored("This test should only be run periodically to avoid API overuse")
@MicronautTest
class SunriseSunsetApiClientTest(private val api: SunriseSunsetApiClient, private val timeHelper: TimeHelper) : StringSpec({

    "Get sunrise sunset times from an API" {
        val lat = "41.5255"
        val long = "-87.3740"
        val ymdString = "2024-12-30"
        val sunriseSunsetModel = api.getSunriseSunsetTimes(lat, long, ymdString).get()
        val sunriseTime = timeHelper.utcTimestampToZoneDateTime(sunriseSunsetModel.results.sunrise)

        sunriseTime.zone.normalized() shouldBe ZoneOffset.systemDefault().normalized()
        sunriseTime.hour shouldBe 7
        sunriseTime.minute shouldBe 14
    }
})