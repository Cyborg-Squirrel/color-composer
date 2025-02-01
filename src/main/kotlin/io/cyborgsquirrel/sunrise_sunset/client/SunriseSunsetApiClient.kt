package io.cyborgsquirrel.sunrise_sunset.client

import io.cyborgsquirrel.sunrise_sunset.model.SunriseSunsetModel
import io.micronaut.core.async.annotation.SingleResult
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client
import java.util.concurrent.Future

// Api readme: https://sunrise-sunset.org/api
@Client("https://api.sunrise-sunset.org")
interface SunriseSunsetApiClient {

    @Get("/json?lat={latitude}&lng={longitude}&date={ymdString}&formatted=0")
    @SingleResult
    fun getSunriseSunsetTimes(
        latitude: String,
        longitude: String,
        ymdString: String
    ): Future<SunriseSunsetModel>
}