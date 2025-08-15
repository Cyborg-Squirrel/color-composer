package io.cyborgsquirrel.clients.api

import io.cyborgsquirrel.clients.requests.CreateClientRequest
import io.cyborgsquirrel.clients.requests.UpdateClientRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*

interface LedClientApi {
    @Get("/{uuid}")
    fun getClient(uuid: String): HttpResponse<Any>

    @Get
    fun getAllClients(): HttpResponse<Any>

    @Post
    fun create(@Body request: CreateClientRequest): HttpResponse<Any>

    @Patch("/{uuid}")
    fun update(uuid: String, @Body request: UpdateClientRequest): HttpResponse<Any>

    @Delete("/{uuid}")
    fun deleteClient(uuid: String): HttpResponse<Any>
}
