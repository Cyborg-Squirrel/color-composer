package io.cyborgsquirrel.setup.api

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get

interface SetupStatusApi {
    @Get("/setup-status")
    fun setupStatus(): HttpResponse<Any>
}