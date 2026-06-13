package io.cyborgsquirrel.clients.command.handler

import io.cyborgsquirrel.clients.command.CreateClientCommand
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.clients.shared.ClientResponseAssembler
import io.cyborgsquirrel.event_source.model.LedClientEvent
import io.cyborgsquirrel.event_source.service.SseEventEmitter
import io.cyborgsquirrel.jobs.streaming.StreamJobManager
import io.cyborgsquirrel.messaging.CommandHandler
import io.cyborgsquirrel.messaging.CommandResponse
import io.cyborgsquirrel.util.exception.ClientRequestException
import jakarta.inject.Singleton
import java.util.*

@Singleton
class CreateClientCommandHandler(
    private val clientRepository: LedStripClientRepository,
    private val streamJobManager: StreamJobManager,
    private val sseEventEmitter: SseEventEmitter,
    private val assembler: ClientResponseAssembler,
) : CommandHandler<CreateClientCommand> {

    override val commandType = CreateClientCommand::class.java

    override fun handle(command: CreateClientCommand): CommandResponse {
        val request = command.request
        val entityOptional = clientRepository.findByAddress(request.address)
        return if (entityOptional.isPresent) {
            throw ClientRequestException("A client with address ${request.address} already exists!")
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
                    firmwareVersion = LedStripClientEntity.DEFAULT_FIRMWARE_VERSION,
                    fps = request.fps ?: LedStripClientEntity.DEFAULT_FPS,
                    fadeTimeoutMillis = request.fadeTimeoutMillis ?: LedStripClientEntity.DEFAULT_FADE_TIMEOUT_MILLIS,
                )
            )

            streamJobManager.startStreamingJob(clientEntity)
            sseEventEmitter.emit(
                LedClientEvent.LedClientCreated(clientEntity.uuid, assembler.mapClientEntityToResponse(clientEntity))
            )
            CommandResponse(clientEntity.uuid)
        }
    }
}
