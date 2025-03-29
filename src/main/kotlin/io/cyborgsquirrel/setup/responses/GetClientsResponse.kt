package io.cyborgsquirrel.setup.responses

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetClientsResponse(val clients: List<GetClientResponse>)