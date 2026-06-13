package io.cyborgsquirrel.clients.command.handler

import io.cyborgsquirrel.clients.command.DeleteClientCommand
import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.jobs.streaming.StreamJobManager
import io.cyborgsquirrel.led_strips.repository.LedStripRepository
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.cyborgsquirrel.test_helpers.saveLedStrip
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.cyborgsquirrel.util.exception.ResourceNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.kotest5.MicronautKotest5Extension.getMock
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.mockk
import io.mockk.verify

@MicronautTest
class DeleteClientCommandHandlerTest(
    private val deleteClientCommandHandler: DeleteClientCommandHandler,
    private val clientRepository: LedStripClientRepository,
    private val stripRepository: LedStripRepository,
    private val streamJobManager: StreamJobManager
) : StringSpec({

    lateinit var mockStreamJobManager: StreamJobManager

    beforeTest {
        mockStreamJobManager = getMock(streamJobManager)
    }

    afterTest {
        stripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Delete client with no strips removes it and stops its streaming job" {
        val client = createLedStripClientEntity(
            clientRepository,
            "Test Client",
            "192.168.1.100",
            8000,
            8001
        )

        deleteClientCommandHandler.handle(DeleteClientCommand(client.uuid))

        verify(atLeast = 1) { mockStreamJobManager.stopWebsocketJob(any()) }
        clientRepository.findByUuid(client.uuid).isPresent shouldBe false
    }

    "Delete client with strips throws ClientRequestException and leaves it in place" {
        val client = createLedStripClientEntity(
            clientRepository,
            "Test Client",
            "192.168.1.100",
            8000,
            8001
        )
        saveLedStrip(stripRepository, client, "Test Strip", 60, "18", 100)

        shouldThrow<ClientRequestException> {
            deleteClientCommandHandler.handle(DeleteClientCommand(client.uuid))
        }

        verify(exactly = 0) { mockStreamJobManager.stopWebsocketJob(any()) }
        clientRepository.findByUuid(client.uuid).isPresent shouldBe true
    }

    "Delete client that does not exist throws ResourceNotFoundException" {
        shouldThrow<ResourceNotFoundException> {
            deleteClientCommandHandler.handle(DeleteClientCommand("does-not-exist"))
        }

        verify(exactly = 0) { mockStreamJobManager.stopWebsocketJob(any()) }
    }
}) {
    @MockBean(StreamJobManager::class)
    fun streamJobManager(): StreamJobManager = mockk(relaxed = true)
}
