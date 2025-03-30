package io.cyborgsquirrel.setup.responses

import io.micronaut.serde.annotation.Serdeable

@Serdeable
class SetupStatusResponse(val status: SetupStatus)