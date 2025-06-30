package io.cyborgsquirrel.server_status.responses

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class SetupStatusResponse(val status: SetupStatus)