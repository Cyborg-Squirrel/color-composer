package io.cyborgsquirrel.model.requests.discovery

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class SelectClientsRequest(val clients: List<SelectedClient>)