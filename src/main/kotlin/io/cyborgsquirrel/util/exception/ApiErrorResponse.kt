package io.cyborgsquirrel.util.exception

import io.micronaut.serde.annotation.Serdeable

/**
 * Structured error body returned for handled api exceptions.
 */
@Serdeable
data class ApiErrorResponse(
    val message: String,
)
