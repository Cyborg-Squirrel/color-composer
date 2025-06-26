package io.cyborgsquirrel.clients.controller

import io.cyborgsquirrel.clients.api.LedClientSetupApi
import io.cyborgsquirrel.clients.requests.CreateClientRequest
import io.cyborgsquirrel.clients.requests.UpdateClientRequest
import io.cyborgsquirrel.clients.service.LedClientSetupApiService
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller

@Controller("/client")
class LedClientSetupController(private val service: LedClientSetupApiService) : LedClientSetupApi {

    override fun getClient(uuid: String): HttpResponse<Any> {
        return try {
            val client = service.getClientWithUuid(uuid)
            HttpResponse.ok(client)
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun getAllClients(): HttpResponse<Any> {
        return try {
            val client = service.getAllClients()
            HttpResponse.ok(client)
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun create(@Body request: CreateClientRequest): HttpResponse<Any> {
        return try {
            val client = service.createClient(request)
            HttpResponse.created(client)
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun update(uuid: String, @Body request: UpdateClientRequest): HttpResponse<Any> {
        return try {
            service.updateClient(uuid, request)
            HttpResponse.noContent()
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }

    override fun deleteClient(uuid: String): HttpResponse<Any> {
        return try {
            service.deleteClient(uuid)
            HttpResponse.noContent()
        } catch (cre: ClientRequestException) {
            HttpResponse.badRequest(cre.message)
        } catch (ex: Exception) {
            HttpResponse.serverError(ex.message)
        }
    }
}