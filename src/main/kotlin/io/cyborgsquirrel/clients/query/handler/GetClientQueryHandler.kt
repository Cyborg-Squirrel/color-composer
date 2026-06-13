package io.cyborgsquirrel.clients.query.handler

import io.cyborgsquirrel.clients.query.GetClientQuery
import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.clients.responses.GetClientResponse
import io.cyborgsquirrel.clients.shared.ClientResponseAssembler
import io.cyborgsquirrel.messaging.QueryHandler
import io.cyborgsquirrel.util.exception.ResourceNotFoundException
import jakarta.inject.Singleton

@Singleton
class GetClientQueryHandler(
    private val clientRepository: LedStripClientRepository,
    private val assembler: ClientResponseAssembler,
) : QueryHandler<GetClientQuery, GetClientResponse> {

    override val queryType = GetClientQuery::class.java

    override fun handle(query: GetClientQuery): GetClientResponse {
        // Join-free finder (CQRS read path): the response only needs scalar columns + uuid, never `strips`.
        val clientEntityOptional = clientRepository.getByUuid(query.uuid)
        if (clientEntityOptional.isPresent) {
            return assembler.mapClientEntityToResponse(clientEntityOptional.get())
        } else {
            throw ResourceNotFoundException("Client with uuid ${query.uuid} doesn't exist!")
        }
    }
}
