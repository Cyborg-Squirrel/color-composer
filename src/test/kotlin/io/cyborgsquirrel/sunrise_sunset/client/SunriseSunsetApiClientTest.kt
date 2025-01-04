package io.cyborgsquirrel.sunrise_sunset.client

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


@Ignored("This test should only be run periodically to avoid API overuse")
@MicronautTest
class SunriseSunsetApiClientTest(private val api: SunriseSunsetApiClient) : StringSpec({

    "Get sunrise sunset times from an API" {
        val lat = "44.5855"
        val long = "-93.160900"
        val ymdString = "2024-12-30"
        val sunriseSunsetModel = api.getSunriseSunsetTimes(lat, long, ymdString).get()
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val sunriseTime = ZonedDateTime.parse(sunriseSunsetModel.results.sunrise, formatter)

        sunriseTime.zone.normalized() shouldBe ZoneOffset.UTC.normalized()
    }
})