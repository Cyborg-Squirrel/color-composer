package io.cyborgsquirrel.clients.discovery.model

import io.cyborgsquirrel.clients.discovery.enums.DiscoveryJobStatus
import io.micronaut.serde.annotation.Serdeable

@Serdeable
class DiscoveryStatusResponse(val status: DiscoveryJobStatus)