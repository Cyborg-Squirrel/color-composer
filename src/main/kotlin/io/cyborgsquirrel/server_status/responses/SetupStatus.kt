package io.cyborgsquirrel.server_status.responses

import io.micronaut.serde.annotation.Serdeable

@Serdeable
enum class SetupStatus {
    // No LED strip clients have been configured
    NoClients,

    // No LED strips have been configured, but at least one client has been configured
    NoStrips,

    // At least one LED strip and one and client have been configured, but no light effects have been configured
    NoEffects,

    // At least one client, strip, and light effect have been configured
    SetupComplete,
}