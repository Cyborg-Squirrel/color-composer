package io.cyborgsquirrel.setup.controller

import io.cyborgsquirrel.client_config.repository.H2LedStripClientRepository
import io.cyborgsquirrel.entity.LedStripClientEntity
import io.cyborgsquirrel.setup.requests.CreateClientRequest
import io.cyborgsquirrel.setup.requests.UpdateClientRequest
import io.cyborgsquirrel.setup.responses.GetClientResponse
import io.cyborgsquirrel.setup.responses.GetClientsResponse
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.Put
import java.util.*

@Controller
class LedClientController(private val clientRepository: H2LedStripClientRepository) {

    @Get("client/{clientUuid}")
    fun getClient(clientUuid: String): HttpResponse<Any> {
        val clientEntityOptional = clientRepository.findByUuid(clientUuid)
        return if (clientEntityOptional.isPresent) {
            val clientEntity = clientEntityOptional.get()
            val clientResponse = GetClientResponse(
                clientEntity.name!!,
                clientEntity.address!!,
                clientEntity.uuid!!,
                clientEntity.apiPort!!,
                clientEntity.wsPort!!
            )

            HttpResponse.ok(clientResponse)
        } else {
            HttpResponse.badRequest("No client exists with uuid $clientUuid!")
        }
    }

    @Get("clients")
    fun getAllClients(): HttpResponse<Any> {
        val clientEntities = clientRepository.findAll()
        val responseClients = clientEntities.map {
            GetClientResponse(it.name!!, it.address!!, it.uuid!!, it.apiPort!!, it.wsPort!!)
        }
        return HttpResponse.ok(GetClientsResponse(responseClients))
    }

    @Put("client")
    fun create(newClient: CreateClientRequest): HttpResponse<Any> {
        val entityOptional = clientRepository.findByAddress(newClient.address)
        return if (entityOptional.isPresent) {
            HttpResponse.ok(entityOptional.get().uuid)
        } else {
            val clientEntity = clientRepository.save(
                LedStripClientEntity(
                    name = newClient.name,
                    address = newClient.address,
                    apiPort = newClient.apiPort,
                    wsPort = newClient.apiPort,
                    uuid = UUID.randomUUID().toString()
                )
            )

            HttpResponse.created(clientEntity.uuid)
        }
    }

    @Patch("client/{clientUuid}")
    fun update(clientUuid: String, updatedClient: UpdateClientRequest): HttpResponse<Any> {
        val entityOptional = clientRepository.findByUuid(clientUuid)

        // TODO validation and notify WebSocket jobs of port number changes
        return if (entityOptional.isPresent) {
            val entity = entityOptional.get()
            var newEntity: LedStripClientEntity? = null
            if (updatedClient.name != null) {
                newEntity = entity.copy(name = updatedClient.name)
            }

            if (updatedClient.address != null) {
                newEntity = entity.copy(address = updatedClient.address)
            }

            if (updatedClient.apiPort != null) {
                newEntity = entity.copy(apiPort = updatedClient.apiPort)
            }

            if (updatedClient.wsPort != null) {
                newEntity = entity.copy(wsPort = updatedClient.wsPort)
            }

            if (newEntity != entity) {
                clientRepository.update(newEntity)
            }

            HttpResponse.noContent()
        } else {
            HttpResponse.badRequest("Client with uuid $clientUuid does not exist! Please create it first before updating it.")
        }
    }
}