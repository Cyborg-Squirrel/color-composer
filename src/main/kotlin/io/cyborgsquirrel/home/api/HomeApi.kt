package io.cyborgsquirrel.home.api

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get

interface HomeApi {
    @Get
    fun getHome(): HttpResponse<Any>
}
