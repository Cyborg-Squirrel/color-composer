package io.cyborgsquirrel.home.controller

import io.cyborgsquirrel.home.api.HomeApi
import io.cyborgsquirrel.home.services.HomeApiService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller

@Controller("/home")
class HomeController(private val service: HomeApiService) : HomeApi {

    override fun getHome(): HttpResponse<Any> {
        return try {
            HttpResponse.ok(service.getHome())
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }
}
