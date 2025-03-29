package io.cyborgsquirrel.setup.responses.client

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GetClientResponse(val name: String, val address: String, val uuid: String, val apiPort: Int, val wsPort: Int)