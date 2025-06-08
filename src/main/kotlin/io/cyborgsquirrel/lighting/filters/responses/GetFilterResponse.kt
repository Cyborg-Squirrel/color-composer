package io.cyborgsquirrel.lighting.filters.responses

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetFilterResponse(
    val name: String,
    val type: String,
    val uuid: String,
    val effectUuid: String?,
    val settings: Map<String, Any>,
)