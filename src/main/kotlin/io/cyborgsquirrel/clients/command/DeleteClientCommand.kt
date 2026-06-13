package io.cyborgsquirrel.clients.command

import io.cyborgsquirrel.messaging.Command

/** Deletes a client (only if it has no strips) and stops its streaming job. */
data class DeleteClientCommand(val uuid: String) : Command
