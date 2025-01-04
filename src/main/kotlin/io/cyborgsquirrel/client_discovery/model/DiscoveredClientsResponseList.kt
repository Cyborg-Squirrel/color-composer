package io.cyborgsquirrel.client_discovery.model

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class DiscoveredClientsResponseList(val clients: Set<ClientDiscoveryResponse>)