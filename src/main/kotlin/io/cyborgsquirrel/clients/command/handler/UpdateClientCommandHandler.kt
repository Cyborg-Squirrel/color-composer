package io.cyborgsquirrel.clients.command.handler

import io.cyborgsquirrel.clients.command.UpdateClientCommand
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.event_source.model.LedClientEvent
import io.cyborgsquirrel.event_source.model.delta.ClientDelta
import io.cyborgsquirrel.event_source.service.SseEventEmitter
import io.cyborgsquirrel.jobs.streaming.StreamJobManager
import io.cyborgsquirrel.messaging.CommandHandler
import io.cyborgsquirrel.messaging.CommandResponse
import io.cyborgsquirrel.util.exception.ResourceNotFoundException
import jakarta.inject.Singleton

@Singleton
class UpdateClientCommandHandler(
    private val clientRepository: LedStripClientRepository,
    private val streamJobManager: StreamJobManager,
    private val sseEventEmitter: SseEventEmitter,
) : CommandHandler<UpdateClientCommand> {

    override val commandType = UpdateClientCommand::class.java

    override fun handle(command: UpdateClientCommand): CommandResponse {
        val uuid = command.uuid
        val request = command.request
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
                fps = request.fps ?: entity.fps,
                fadeTimeoutMillis = request.fadeTimeoutMillis ?: entity.fadeTimeoutMillis,
            )

            if (shouldRestartStreamingJob(entity, newEntity)) {
                streamJobManager.stopWebsocketJob(entity)
                clientRepository.update(newEntity)
                streamJobManager.startStreamingJob(newEntity)
            } else {
                clientRepository.update(newEntity)
            }
            val delta = LedClientEvent.LedClientUpdated(
                uuid,
                ClientDelta(
                    name = newEntity.name.takeIf { it != entity.name },
                    address = newEntity.address.takeIf { it != entity.address },
                    colorOrder = newEntity.colorOrder.takeIf { it != entity.colorOrder },
                    apiPort = newEntity.apiPort.takeIf { it != entity.apiPort },
                    wsPort = newEntity.wsPort.takeIf { it != entity.wsPort },
                    powerLimit = newEntity.powerLimit.takeIf { it != entity.powerLimit },
                    fps = newEntity.fps.takeIf { it != entity.fps },
                    fadeTimeoutMillis = newEntity.fadeTimeoutMillis.takeIf { it != entity.fadeTimeoutMillis },
                )
            )
            sseEventEmitter.emit(delta)
            return CommandResponse(command.uuid)
        } else {
            throw ResourceNotFoundException("Client with uuid $uuid does not exist! Please create it first before updating it.")
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
        val fpsChanged = oldClientEntity.fps != newClientEntity.fps
        val fadeTimeoutChanged = oldClientEntity.fadeTimeoutMillis != newClientEntity.fadeTimeoutMillis
        return clientTypeChanged || powerLimitChanged || apiPortChanged || wsPortChanged || stripsChanged || addressChanged || colorOrderChanged || fpsChanged || fadeTimeoutChanged
    }
}
