package io.cyborgsquirrel.setup.requests

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class SelectedClientRequest(val name: String, val address: String)