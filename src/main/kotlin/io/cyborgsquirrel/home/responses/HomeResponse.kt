package io.cyborgsquirrel.home.responses

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class HomeResponse(
    val clients: Long,
    val strips: Long,
    val effects: Long,
    val palettes: Long,
)
