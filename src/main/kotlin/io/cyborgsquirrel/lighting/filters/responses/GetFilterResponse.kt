package io.cyborgsquirrel.lighting.filters.responses

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetFilterResponse(
    val name: String,
    val type: String,
    val uuid: String,
    val effectUuids: List<String>,
    val settings: Map<String, Any>,
)