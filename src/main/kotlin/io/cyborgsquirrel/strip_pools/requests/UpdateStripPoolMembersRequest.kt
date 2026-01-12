package io.cyborgsquirrel.strip_pools.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class UpdateStripPoolMembersRequest(
    val members: List<StripPoolMemberRequestModel>
)