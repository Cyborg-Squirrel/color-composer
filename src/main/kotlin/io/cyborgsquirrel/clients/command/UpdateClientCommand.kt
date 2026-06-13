package io.cyborgsquirrel.clients.command

import io.cyborgsquirrel.clients.requests.UpdateClientRequest
import io.cyborgsquirrel.messaging.Command

/** Updates a client, restarting its streaming job if stream-affecting fields changed. */
data class UpdateClientCommand(val uuid: String, val request: UpdateClientRequest) : Command
