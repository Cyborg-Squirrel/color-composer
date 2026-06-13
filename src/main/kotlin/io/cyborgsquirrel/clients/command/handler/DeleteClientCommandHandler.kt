package io.cyborgsquirrel.clients.command.handler

import io.cyborgsquirrel.clients.command.DeleteClientCommand
import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.event_source.model.LedClientEvent
import io.cyborgsquirrel.event_source.service.SseEventEmitter
import io.cyborgsquirrel.jobs.streaming.StreamJobManager
import io.cyborgsquirrel.messaging.CommandHandler
import io.cyborgsquirrel.messaging.CommandResponse
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.cyborgsquirrel.util.exception.ResourceNotFoundException
import jakarta.inject.Singleton

@Singleton
class DeleteClientCommandHandler(
    private val clientRepository: LedStripClientRepository,
    private val streamJobManager: StreamJobManager,
    private val sseEventEmitter: SseEventEmitter,
) : CommandHandler<DeleteClientCommand> {

    override val commandType = DeleteClientCommand::class.java

    override fun handle(command: DeleteClientCommand): CommandResponse {
        val uuid = command.uuid
        val entityOptional = clientRepository.findByUuid(uuid)
        if (entityOptional.isPresent) {
            val entity = entityOptional.get()
            if (entity.strips.isEmpty()) {
                streamJobManager.stopWebsocketJob(entity)
                clientRepository.deleteById(entity.id)
                sseEventEmitter.emit(LedClientEvent.LedClientDeleted(uuid))
                return CommandResponse(command.uuid)
            } else {
                throw ClientRequestException("Could not delete client with uuid $uuid. Please delete its LED strips first.")
            }
        } else {
            throw ResourceNotFoundException("Could not delete client with uuid $uuid. It does not exist.")
        }
    }
}
