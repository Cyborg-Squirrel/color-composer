package io.cyborgsquirrel.setup.responses.status

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class SetupStatusResponse(val status: SetupStatus)