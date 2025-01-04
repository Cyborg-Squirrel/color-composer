package io.cyborgsquirrel.controller

import io.cyborgsquirrel.model.responses.ping.PingResponse
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

@Controller
class PingController {

    @Get("/ping")
    fun ping(): HttpResponse<PingResponse> {
        return HttpResponse.ok(PingResponse("Color Composer"))
    }
}