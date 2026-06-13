package io.cyborgsquirrel.clients.command

import io.cyborgsquirrel.clients.requests.CreateClientRequest
import io.cyborgsquirrel.messaging.Command

/** Creates a client (deduped by address) and starts its streaming job. Responds with the client uuid. */
data class CreateClientCommand(val request: CreateClientRequest) : Command
