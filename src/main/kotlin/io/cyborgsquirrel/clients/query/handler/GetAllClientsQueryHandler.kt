package io.cyborgsquirrel.clients.query.handler

import io.cyborgsquirrel.clients.query.GetAllClientsQuery
import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.clients.responses.GetClientsResponse
import io.cyborgsquirrel.clients.shared.ClientResponseAssembler
import io.cyborgsquirrel.messaging.QueryHandler
import jakarta.inject.Singleton

@Singleton
class GetAllClientsQueryHandler(
    private val clientRepository: LedStripClientRepository,
    private val assembler: ClientResponseAssembler,
) : QueryHandler<GetAllClientsQuery, GetClientsResponse> {

    override val queryType = GetAllClientsQuery::class.java

    override fun handle(query: GetAllClientsQuery): GetClientsResponse {
        // Inherited findAll() is join-free, unlike queryAll() which LEFT_FETCHes `strips` for callers
        // that need the strip graph. The read path uses only scalar columns + uuid.
        val responseClients = clientRepository.findAll().map { assembler.mapClientEntityToResponse(it) }
        return GetClientsResponse(responseClients)
    }
}
