package io.cyborgsquirrel.clients.api

import io.cyborgsquirrel.clients.controller.requests.CreateClientRequest
import io.cyborgsquirrel.clients.controller.requests.UpdateClientRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*

interface LedClientSetupApi {
    @Get("/{uuid}")
    fun getClient(uuid: String): HttpResponse<Any>

    @Get("/all")
    fun getAllClients(): HttpResponse<Any>

    @Put
    fun create(@Body newClient: CreateClientRequest): HttpResponse<Any>

    @Patch("/{uuid}")
    fun update(uuid: String, @Body updatedClient: UpdateClientRequest): HttpResponse<Any>

    @Delete("/{uuid}")
    fun deleteClient(uuid: String): HttpResponse<Any>
}
