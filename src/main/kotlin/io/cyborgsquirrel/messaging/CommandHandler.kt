package io.cyborgsquirrel.messaging

/**
 * Handles a single [Command] type. [commandType] is declared explicitly because Kotlin erases the
 * generic argument, so the [CommandBus] cannot recover it at runtime to route by type.
 */
interface CommandHandler<C : Command> {
    val commandType: Class<C>

    fun handle(command: C): CommandResponse
}
