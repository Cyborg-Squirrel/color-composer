package io.cyborgsquirrel.strip_pools.responses

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetStripPoolsResponse(val pools: List<GetStripPoolResponse>)