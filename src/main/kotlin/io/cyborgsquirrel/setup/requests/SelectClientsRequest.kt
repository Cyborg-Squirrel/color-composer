package io.cyborgsquirrel.setup.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class SelectClientsRequest(val clients: List<SelectedClient>)