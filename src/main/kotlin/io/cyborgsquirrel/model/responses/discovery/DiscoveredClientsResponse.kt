package io.cyborgsquirrel.model.responses.discovery

import io.cyborgsquirrel.model.client.DiscoveryResponse
import io.micronaut.serde.annotation.Serdeable

@Serdeable
class DiscoveredClientsResponse(val clients: List<DiscoveryResponse>)