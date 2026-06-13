package io.cyborgsquirrel.messaging

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private data class EchoCommand(val value: String) : Command
private data class CountCommand(val value: Int) : Command
private data class UnregisteredCommand(val value: String) : Command

private class CountCommandHandler : CommandHandler<CountCommand> {
    override val commandType = CountCommand::class.java
    override fun handle(command: CountCommand) = CommandResponse((command.value + 2).toString())
}

private class EchoCommandHandler : CommandHandler<EchoCommand> {
    override val commandType = EchoCommand::class.java
    override fun handle(command: EchoCommand) = CommandResponse("echo:${command.value}")
}

class CommandBusImplTest : StringSpec({

    "routes each command to its registered handler by type" {
        val bus = CommandBusImpl(listOf(EchoCommandHandler(), CountCommandHandler()))

        bus.send(EchoCommand("hi")) shouldBe CommandResponse("echo:hi")
        bus.send(CountCommand(21)) shouldBe CommandResponse("23")
    }

    "throws when no handler is registered for the command type" {
        val bus = CommandBusImpl(listOf(EchoCommandHandler()))

        shouldThrow<IllegalStateException> {
            bus.send(UnregisteredCommand("x"))
        }
    }
})
