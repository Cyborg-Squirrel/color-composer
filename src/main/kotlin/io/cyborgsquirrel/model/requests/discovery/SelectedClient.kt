package io.cyborgsquirrel.model.requests.discovery

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class SelectedClient(val name: String, val address: String)