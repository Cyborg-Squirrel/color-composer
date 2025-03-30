package io.cyborgsquirrel.clients.responses

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetClientsResponse(val clients: List<GetClientResponse>)