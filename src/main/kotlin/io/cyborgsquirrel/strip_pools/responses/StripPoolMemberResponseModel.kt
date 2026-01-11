package io.cyborgsquirrel.strip_pools.responses

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class StripPoolMemberResponseModel(
    val uuid: String,
    val stripUuid: String,
    val inverted: Boolean,
    val poolIndex: Int
)