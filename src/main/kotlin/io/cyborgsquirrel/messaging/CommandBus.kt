package io.cyborgsquirrel.messaging

/**
 * Resolves the [CommandHandler] for a given [Command] and invokes it. Controllers depend only on
 * this, never on concrete handlers.
 */
interface CommandBus {
    fun send(command: Command): CommandResponse
}
