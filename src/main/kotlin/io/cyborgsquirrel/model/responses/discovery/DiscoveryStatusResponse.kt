package io.cyborgsquirrel.model.responses.discovery

import io.cyborgsquirrel.job.enums.DiscoveryJobStatus
import io.micronaut.serde.annotation.Serdeable

@Serdeable
class DiscoveryStatusResponse(val status: DiscoveryJobStatus)