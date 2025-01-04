package io.cyborgsquirrel.model.client

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class DiscoveryResponse(val wsPort: Int, val apiPort: Int, val name: String) {

    lateinit var address: String
}