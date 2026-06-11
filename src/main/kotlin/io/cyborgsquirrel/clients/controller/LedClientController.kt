package io.cyborgsquirrel.clients.controller

import io.cyborgsquirrel.clients.api.LedClientApi
import io.cyborgsquirrel.clients.requests.CreateClientRequest
import io.cyborgsquirrel.clients.requests.UpdateClientRequest
import io.cyborgsquirrel.clients.services.LedClientApiService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller

@Controller("/client")
class LedClientController(private val service: LedClientApiService) : LedClientApi {

    override fun getClient(uuid: String): HttpResponse<Any> {
        return HttpResponse.ok(service.getClientWithUuid(uuid))
    }

    override fun getAllClients(): HttpResponse<Any> {
        return HttpResponse.ok(service.getAllClients())
    }

    override fun create(@Body request: CreateClientRequest): HttpResponse<Any> {
        return HttpResponse.created(service.createClient(request))
    }

    override fun update(uuid: String, @Body request: UpdateClientRequest): HttpResponse<Any> {
        service.updateClient(uuid, request)
        return HttpResponse.noContent()
    }

    override fun deleteClient(uuid: String): HttpResponse<Any> {
        service.deleteClient(uuid)
        return HttpResponse.noContent()
    }
}
