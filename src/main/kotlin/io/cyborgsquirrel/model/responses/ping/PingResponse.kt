package io.cyborgsquirrel.model.responses.ping

import io.micronaut.serde.annotation.SerdeImport

@SerdeImport
class PingResponse(val name: String)