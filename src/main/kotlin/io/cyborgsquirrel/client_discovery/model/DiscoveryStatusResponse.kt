package io.cyborgsquirrel.client_discovery.model

import io.cyborgsquirrel.client_discovery.enums.DiscoveryJobStatus
import io.micronaut.serde.annotation.Serdeable

@Serdeable
class DiscoveryStatusResponse(val status: DiscoveryJobStatus)