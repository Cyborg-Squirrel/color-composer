package io.cyborgsquirrel.clients.services

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.clients.requests.CreateClientRequest
import io.cyborgsquirrel.clients.requests.UpdateClientRequest
import io.cyborgsquirrel.clients.responses.GetClientResponse
import io.cyborgsquirrel.clients.responses.GetClientsResponse
import io.cyborgsquirrel.clients.status.ClientStatusInfo
import io.cyborgsquirrel.clients.status.ClientStatusService
import io.cyborgsquirrel.jobs.streaming.StreamJobManager
import io.cyborgsquirrel.util.exception.ClientRequestException
import jakarta.inject.Singleton
import java.util.*

@Singleton
class LedClientApiService(
    private val clientRepository: H2LedStripClientRepository,
    private val streamJobManager: StreamJobManager,
    private val clientStatusService: ClientStatusService,
) {

    fun getAllClients(): GetClientsResponse {
        val clientEntities = clientRepository.queryAll()
        val responseClients = clientEntities.map {
            mapClientEntityToResponse(it)
        }
        return GetClientsResponse(responseClients)
    }

    fun getClientWithUuid(uuid: String): GetClientResponse {
        val clientEntityOptional = clientRepository.findByUuid(uuid)
        if (clientEntityOptional.isPresent) {
            val clientEntity = clientEntityOptional.get()
            return mapClientEntityToResponse(clientEntity)
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
                    uuid = UUID.randomUUID().toString(),
                    powerLimit = request.powerLimit ?: 0,
                    firmwareVersion = if (request.clientType == ClientType.Pi) "0.1" else "--",
                )
            )

            streamJobManager.startStreamingJob(clientEntity)
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
                powerLimit = request.powerLimit ?: entity.powerLimit,
            )

            if (shouldRestartStreamingJob(entity, newEntity)) {
                streamJobManager.stopWebsocketJob(entity)
                clientRepository.update(newEntity)
                streamJobManager.startStreamingJob(newEntity)
            } else {
                clientRepository.update(newEntity)
            }
        } else {
            throw ClientRequestException("Client with uuid $uuid does not exist! Please create it first before updating it.")
        }
    }

    private fun shouldRestartStreamingJob(
        oldClientEntity: LedStripClientEntity,
        newClientEntity: LedStripClientEntity
    ): Boolean {
        val clientTypeChanged = oldClientEntity.clientType != newClientEntity.clientType
        val powerLimitChanged = oldClientEntity.powerLimit != newClientEntity.powerLimit
        val apiPortChanged = oldClientEntity.apiPort != newClientEntity.apiPort
        val wsPortChanged = oldClientEntity.wsPort != newClientEntity.wsPort
        val stripsChanged = oldClientEntity.strips.map { it.uuid } != newClientEntity.strips.map { it.uuid }
        val addressChanged = oldClientEntity.address != newClientEntity.address
        val colorOrderChanged = oldClientEntity.colorOrder != newClientEntity.colorOrder
        return clientTypeChanged || powerLimitChanged || apiPortChanged || wsPortChanged || stripsChanged || addressChanged || colorOrderChanged
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

    fun mapClientEntityToResponse(client: LedStripClientEntity): GetClientResponse {
        val statusInfo = getStatusInfo(client)
        return GetClientResponse(
            client.name!!,
            client.address!!,
            client.uuid!!,
            client.clientType.toString(),
            client.colorOrder!!,
            client.apiPort!!,
            client.wsPort!!,
            client.lastSeenAt,
            statusInfo.status,
            statusInfo.activeEffects,
            client.powerLimit,
            client.firmwareVersion!!,
        )
    }

    private fun getStatusInfo(client: LedStripClientEntity): ClientStatusInfo {
        val statusOptional = clientStatusService.getStatusForClient(client)
        return if (statusOptional.isPresent) {
            statusOptional.get()
        } else {
            ClientStatusInfo.error()
        }
    }
}