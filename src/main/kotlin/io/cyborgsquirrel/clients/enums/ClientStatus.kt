package io.cyborgsquirrel.clients.enums

import io.micronaut.serde.annotation.Serdeable

@Serdeable
enum class ClientStatus {
    // Client has been created but has no LED strips configured
    SetupIncomplete,

    // Client is connected, is not in an error state, has at least one LED strip configured
    Idle,

    // Client is connected with one or more effect playing or paused
    Active,

    // Client is disconnected and reconnect attempts are not being attempted
    Disconnected,

    // Client is in an error state
    Error,
}