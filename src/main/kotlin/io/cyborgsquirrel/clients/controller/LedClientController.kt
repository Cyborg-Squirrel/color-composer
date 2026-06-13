package io.cyborgsquirrel.clients.controller

import io.cyborgsquirrel.clients.api.LedClientApi
import io.cyborgsquirrel.clients.command.CreateClientCommand
import io.cyborgsquirrel.clients.command.DeleteClientCommand
import io.cyborgsquirrel.clients.command.UpdateClientCommand
import io.cyborgsquirrel.clients.query.GetAllClientsQuery
import io.cyborgsquirrel.clients.query.GetClientQuery
import io.cyborgsquirrel.clients.requests.CreateClientRequest
import io.cyborgsquirrel.clients.requests.UpdateClientRequest
import io.cyborgsquirrel.messaging.CommandBus
import io.cyborgsquirrel.messaging.QueryBus
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller

@Controller("/client")
class LedClientController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
) : LedClientApi {

    override fun getClient(uuid: String): HttpResponse<Any> {
        return HttpResponse.ok(queryBus.send(GetClientQuery(uuid)))
    }

    override fun getAllClients(): HttpResponse<Any> {
        return HttpResponse.ok(queryBus.send(GetAllClientsQuery()))
    }

    override fun create(@Body request: CreateClientRequest): HttpResponse<Any> {
        val response = commandBus.send(CreateClientCommand(request))
        return HttpResponse.created(response.uuid)
    }

    override fun update(uuid: String, @Body request: UpdateClientRequest): HttpResponse<Any> {
        commandBus.send(UpdateClientCommand(uuid, request))
        return HttpResponse.noContent()
    }

    override fun deleteClient(uuid: String): HttpResponse<Any> {
        commandBus.send(DeleteClientCommand(uuid))
        return HttpResponse.noContent()
    }
}
