package io.cyborgsquirrel.clients.discovery.model

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class DiscoveredClientsResponseList(val clients: Set<ClientDiscoveryResponse>)