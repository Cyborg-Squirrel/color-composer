package io.cyborgsquirrel.server_status.api

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get

interface ServerStatusApi {
    @Get("/setup-status")
    fun setupStatus(): HttpResponse<Any>

    @Get("/version")
    fun getVersion(): HttpResponse<String>
}