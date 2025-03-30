package io.cyborgsquirrel.clients.controller

import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.api.LedClientSetupApi
import io.cyborgsquirrel.clients.requests.CreateClientRequest
import io.cyborgsquirrel.clients.requests.UpdateClientRequest
import io.cyborgsquirrel.clients.responses.GetClientResponse
import io.cyborgsquirrel.clients.responses.GetClientsResponse
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import java.util.*

@Controller("/client")
class LedClientSetupController(private val clientRepository: H2LedStripClientRepository) : LedClientSetupApi {

    override fun getClient(uuid: String): HttpResponse<Any> {
        val clientEntityOptional = clientRepository.findByUuid(uuid)
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
            HttpResponse.badRequest("No client exists with uuid $uuid!")
        }
    }

    override fun getAllClients(): HttpResponse<Any> {
        val clientEntities = clientRepository.findAll()
        val responseClients = clientEntities.map {
            GetClientResponse(it.name!!, it.address!!, it.uuid!!, it.apiPort!!, it.wsPort!!)
        }
        return HttpResponse.ok(GetClientsResponse(responseClients))
    }

    override fun create(@Body newClient: CreateClientRequest): HttpResponse<Any> {
        val entityOptional = clientRepository.findByAddress(newClient.address)
        return if (entityOptional.isPresent) {
            HttpResponse.ok(entityOptional.get().uuid)
        } else {
            val clientEntity = clientRepository.save(
                LedStripClientEntity(
                    name = newClient.name,
                    address = newClient.address,
                    apiPort = newClient.apiPort,
                    wsPort = newClient.wsPort,
                    uuid = UUID.randomUUID().toString()
                )
            )

            HttpResponse.created(clientEntity.uuid)
        }
    }

    override fun update(uuid: String, @Body updatedClient: UpdateClientRequest): HttpResponse<Any> {
        val entityOptional = clientRepository.findByUuid(uuid)

        // TODO validation and notify WebSocket jobs of port number changes
        return if (entityOptional.isPresent) {
            val entity = entityOptional.get()
            val newEntity = entity.copy(
                name = updatedClient.name ?: entity.name,
                address = updatedClient.address ?: entity.address,
                apiPort = updatedClient.apiPort ?: entity.apiPort,
                wsPort = updatedClient.wsPort ?: entity.wsPort,
            )

            if (newEntity != entity) {
                clientRepository.update(newEntity)
            }

            HttpResponse.noContent()
        } else {
            HttpResponse.badRequest("Client with uuid $uuid does not exist! Please create it first before updating it.")
        }
    }

    override fun deleteClient(uuid: String): HttpResponse<Any> {
        val entityOptional = clientRepository.findByUuid(uuid)
        return if (entityOptional.isPresent) {
            val entity = entityOptional.get()
            if (entity.strips.isEmpty()) {
                clientRepository.deleteById(entityOptional.get().id)
                HttpResponse.ok()
            } else {
                // TODO cascading delete? Unassign light effects from client's strips?
                HttpResponse.badRequest("Could not delete client with uuid $uuid. Please delete its LED strips first.")
            }
        } else {
            HttpResponse.badRequest("Could not delete client with uuid $uuid. It does not exist.")
        }
    }
}