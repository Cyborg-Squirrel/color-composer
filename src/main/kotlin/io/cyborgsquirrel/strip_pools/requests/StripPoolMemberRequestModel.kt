package io.cyborgsquirrel.strip_pools.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class StripPoolMemberRequestModel(
    val uuid: String?,
    val stripUuid: String,
    val inverted: Boolean,
    val poolIndex: Int
)