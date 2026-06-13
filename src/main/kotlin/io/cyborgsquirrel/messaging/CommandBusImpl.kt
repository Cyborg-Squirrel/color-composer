package io.cyborgsquirrel.messaging

import jakarta.inject.Singleton

/**
 * Builds a `Class<command> -> handler` map from the injected handlers at construction time
 * (reflection-free routing) and dispatches each command to its handler.
 */
@Singleton
class CommandBusImpl(
    handlers: List<CommandHandler<*>>,
) : CommandBus {

    private val handlersByType: Map<Class<*>, CommandHandler<*>> =
        handlers.associateBy { it.commandType }

    override fun send(command: Command): CommandResponse {
        @Suppress("UNCHECKED_CAST")
        val handler = handlersByType[command::class.java] as? CommandHandler<Command>
            ?: throw IllegalStateException(
                "No command handler registered for ${command::class.java.name}"
            )
        return handler.handle(command)
    }
}
