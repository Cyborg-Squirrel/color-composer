package io.cyborgsquirrel.clients.discovery.model

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class ClientDiscoveryResponse(val wsPort: Int, val apiPort: Int, val name: String) {

    lateinit var address: String
}