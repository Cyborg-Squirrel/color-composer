package io.cyborgsquirrel.clients.command.handler

import io.cyborgsquirrel.clients.command.CreateClientCommand
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.clients.requests.CreateClientRequest
import io.cyborgsquirrel.jobs.streaming.StreamJobManager
import io.cyborgsquirrel.led_strips.repository.LedStripRepository
import io.cyborgsquirrel.messaging.CommandResponse
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.kotest5.MicronautKotest5Extension.getMock
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.mockk
import io.mockk.verify

@MicronautTest
class CreateClientCommandHandlerTest(
    private val createClientCommandHandler: CreateClientCommandHandler,
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

    "Create client persists a new client and starts its streaming job" {
        val request = CreateClientRequest(
            name = "Test Client",
            address = "192.168.1.100",
            clientType = ClientType.Pi,
            colorOrder = ColorOrder.BGR,
            apiPort = 8000,
            wsPort = 8001,
            powerLimit = 75
        )

        val response = createClientCommandHandler.handle(CreateClientCommand(request))

        response.shouldBeInstanceOf<CommandResponse>()
        verify(atLeast = 1) { mockStreamJobManager.startStreamingJob(any()) }

        val savedClient = clientRepository.findByUuid(response.uuid).get()
        savedClient.name shouldBe "Test Client"
        savedClient.address shouldBe "192.168.1.100"
        savedClient.colorOrder shouldBe ColorOrder.BGR
        savedClient.apiPort shouldBe 8000
        savedClient.wsPort shouldBe 8001
        savedClient.powerLimit shouldBe 75
    }

    "Create client defaults color order to RGB when none is specified" {
        val request = CreateClientRequest(
            name = "Test Client",
            address = "192.168.1.101",
            clientType = ClientType.Pi,
            colorOrder = null,
            apiPort = 8000,
            wsPort = 8001,
            powerLimit = null
        )

        val response = createClientCommandHandler.handle(CreateClientCommand(request))

        response.shouldBeInstanceOf<CommandResponse>()
        val savedClient = clientRepository.findByUuid(response.uuid).get()
        savedClient.colorOrder shouldBe ColorOrder.RGB
    }

    "Create client with an existing address throws ClientRequestException without creating a duplicate or starting a job" {
        createLedStripClientEntity(
            clientRepository,
            "Existing Client",
            "192.168.1.100",
            8000,
            8001
        )

        val request = CreateClientRequest(
            name = "Duplicate Client",
            address = "192.168.1.100",  // Same address as existing
            clientType = ClientType.Pi,
            colorOrder = ColorOrder.RGB,
            apiPort = 9000,
            wsPort = 9001,
            powerLimit = null
        )

        shouldThrow<ClientRequestException> {
            createClientCommandHandler.handle(CreateClientCommand(request))
        }

        verify(exactly = 0) { mockStreamJobManager.startStreamingJob(any()) }
        verify(exactly = 0) { mockStreamJobManager.stopWebsocketJob(any()) }

        // The existing client is left untouched and no duplicate was persisted
        clientRepository.queryAll().size shouldBe 1
        val existing = clientRepository.findByAddress("192.168.1.100").get()
        existing.name shouldBe "Existing Client"
        existing.apiPort shouldBe 8000
    }
}) {
    @MockBean(StreamJobManager::class)
    fun streamJobManager(): StreamJobManager = mockk(relaxed = true)
}
