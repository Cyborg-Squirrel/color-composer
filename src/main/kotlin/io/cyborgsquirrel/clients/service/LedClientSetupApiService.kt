package io.cyborgsquirrel.clients.service

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.clients.requests.CreateClientRequest
import io.cyborgsquirrel.clients.requests.UpdateClientRequest
import io.cyborgsquirrel.clients.responses.GetClientResponse
import io.cyborgsquirrel.clients.responses.GetClientsResponse
import io.cyborgsquirrel.jobs.streaming.StreamJobManager
import io.cyborgsquirrel.util.exception.ClientRequestException
import jakarta.inject.Singleton
import java.util.*

@Singleton
class LedClientSetupApiService(
    private val clientRepository: H2LedStripClientRepository,
    private val streamJobManager: StreamJobManager,
) {

    fun getAllClients(): GetClientsResponse {
        val clientEntities = clientRepository.findAll()
        val responseClients = clientEntities.map {
            GetClientResponse(
                it.name!!,
                it.address!!,
                it.uuid!!,
                it.clientType.toString(),
                it.colorOrder!!,
                it.apiPort!!,
                it.wsPort!!,
            )
        }
        return GetClientsResponse(responseClients)
    }

    fun getClientWithUuid(uuid: String): GetClientResponse {
        val clientEntityOptional = clientRepository.findByUuid(uuid)
        if (clientEntityOptional.isPresent) {
            val clientEntity = clientEntityOptional.get()
            val clientResponse = GetClientResponse(
                clientEntity.name!!,
                clientEntity.address!!,
                clientEntity.uuid!!,
                clientEntity.clientType.toString(),
                clientEntity.colorOrder!!,
                clientEntity.apiPort!!,
                clientEntity.wsPort!!,
            )

            return clientResponse
        } else {
            throw ClientRequestException("Client with uuid $uuid doesn't exist!")
        }
    }

    fun createClient(request: CreateClientRequest): String {
        val entityOptional = clientRepository.findByAddress(request.address)
        return if (entityOptional.isPresent) {
            entityOptional.get().uuid!!
        } else {
            // Default to RGB if no color order is specified - it is not required for Pi clients
            val colorOrder = request.colorOrder ?: ColorOrder.RGB

            val clientEntity = clientRepository.save(
                LedStripClientEntity(
                    name = request.name,
                    address = request.address,
                    clientType = request.clientType,
                    colorOrder = colorOrder,
                    apiPort = request.apiPort,
                    wsPort = request.wsPort,
                    uuid = UUID.randomUUID().toString()
                )
            )

            streamJobManager.startWebsocketJob(clientEntity)
            clientEntity.uuid!!
        }
    }

    fun updateClient(uuid: String, request: UpdateClientRequest) {
        val entityOptional = clientRepository.findByUuid(uuid)

        if (entityOptional.isPresent) {
            val entity = entityOptional.get()
            val newEntity = entity.copy(
                name = request.name ?: entity.name,
                address = request.address ?: entity.address,
                colorOrder = request.colorOrder ?: entity.colorOrder,
                apiPort = request.apiPort ?: entity.apiPort,
                wsPort = request.wsPort ?: entity.wsPort,
            )

            if (newEntity != entity) {
                streamJobManager.stopWebsocketJob(entity)
                clientRepository.update(newEntity)
                streamJobManager.startWebsocketJob(newEntity)
            }
        } else {
            throw ClientRequestException("Client with uuid $uuid does not exist! Please create it first before updating it.")
        }
    }

    fun deleteClient(uuid: String) {
        val entityOptional = clientRepository.findByUuid(uuid)
        if (entityOptional.isPresent) {
            val entity = entityOptional.get()
            if (entity.strips.isEmpty()) {
                streamJobManager.stopWebsocketJob(entity)
                clientRepository.deleteById(entityOptional.get().id)
            } else {
                throw ClientRequestException("Could not delete client with uuid $uuid. Please delete its LED strips first.")
            }
        } else {
            throw ClientRequestException("Could not delete client with uuid $uuid. It does not exist.")
        }
    }
}