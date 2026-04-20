package io.cyborgsquirrel.clients.services

import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.clients.requests.UpdateClientRequest
import io.cyborgsquirrel.led_strips.repository.H2LedStripRepository
import io.cyborgsquirrel.jobs.streaming.StreamJobManager
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.kotest5.MicronautKotest5Extension.getMock
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.mockk
import io.mockk.verify

@MicronautTest
class LedClientApiServiceTest(
    private val ledClientApiService: LedClientApiService,
    private val clientRepository: H2LedStripClientRepository,
    private val stripRepository: H2LedStripRepository,
    private val streamJobManager: StreamJobManager
) : StringSpec({

    afterTest {
        stripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "Update client with powerLimit change triggers stream restart" {
        val client = createLedStripClientEntity(
            clientRepository,
            "Test Client",
            "192.168.1.100",
            8000,
            8001,
            powerLimit = 50
        )

        val updateRequest = UpdateClientRequest(
            name = null,
            address = null,
            colorOrder = null,
            apiPort = null,
            wsPort = null,
            powerLimit = 100  // Changed
        )

        ledClientApiService.updateClient(client.uuid!!, updateRequest)

        val streamJobManager = getMock(streamJobManager)
        verify(atLeast = 1) { streamJobManager.stopWebsocketJob(any()) }
        verify(atLeast = 1) { streamJobManager.startStreamingJob(any()) }

        val updatedClient = clientRepository.findByUuid(client.uuid!!).get()
        updatedClient.powerLimit shouldBe 100
    }

    "Update client with api port change triggers stream restart" {
        val client = createLedStripClientEntity(
            clientRepository,
            "Test Client",
            "192.168.1.100",
            8000,
            8001
        )

        val updateRequest = UpdateClientRequest(
            name = null,
            address = null,
            colorOrder = null,
            apiPort = 9000,  // Changed
            wsPort = null,
            powerLimit = null
        )

        ledClientApiService.updateClient(client.uuid!!, updateRequest)

        val streamJobManager = getMock(streamJobManager)
        verify(atLeast = 1) { streamJobManager.stopWebsocketJob(any()) }
        verify(atLeast = 1) { streamJobManager.startStreamingJob(any()) }

        val updatedClient = clientRepository.findByUuid(client.uuid!!).get()
        updatedClient.apiPort shouldBe 9000
    }

    "Update client with websocket port change triggers stream restart" {
        val client = createLedStripClientEntity(
            clientRepository,
            "Test Client",
            "192.168.1.100",
            8000,
            8001
        )

        val updateRequest = UpdateClientRequest(
            name = null,
            address = null,
            colorOrder = null,
            apiPort = null,
            wsPort = 9001,  // Changed
            powerLimit = null
        )

        ledClientApiService.updateClient(client.uuid!!, updateRequest)

        val streamJobManager = getMock(streamJobManager)
        verify(atLeast = 1) { streamJobManager.stopWebsocketJob(any()) }
        verify(atLeast = 1) { streamJobManager.startStreamingJob(any()) }

        val updatedClient = clientRepository.findByUuid(client.uuid!!).get()
        updatedClient.wsPort shouldBe 9001
    }

    "Update client with address change triggers stream restart" {
        val client = createLedStripClientEntity(
            clientRepository,
            "Test Client",
            "192.168.1.100",
            8000,
            8001
        )

        val updateRequest = UpdateClientRequest(
            name = null,
            address = "192.168.1.200",  // Changed
            colorOrder = null,
            apiPort = null,
            wsPort = null,
            powerLimit = null
        )

        ledClientApiService.updateClient(client.uuid!!, updateRequest)

        val streamJobManager = getMock(streamJobManager)
        verify(atLeast = 1) { streamJobManager.stopWebsocketJob(any()) }
        verify(atLeast = 1) { streamJobManager.startStreamingJob(any()) }

        val updatedClient = clientRepository.findByUuid(client.uuid!!).get()
        updatedClient.address shouldBe "192.168.1.200"
    }

    "Update client with color order change triggers stream restart" {
        val client = createLedStripClientEntity(
            clientRepository,
            "Test Client",
            "192.168.1.100",
            8000,
            8001
        )

        val originalColorOrder = client.colorOrder

        val updateRequest = UpdateClientRequest(
            name = null,
            address = null,
            colorOrder = if (originalColorOrder == ColorOrder.RGB) ColorOrder.BGR else ColorOrder.RGB,  // Changed
            apiPort = null,
            wsPort = null,
            powerLimit = null
        )

        ledClientApiService.updateClient(client.uuid!!, updateRequest)

        val streamJobManager = getMock(streamJobManager)
        verify(atLeast = 1) { streamJobManager.stopWebsocketJob(any()) }
        verify(atLeast = 1) { streamJobManager.startStreamingJob(any()) }
    }

    "Update client with only name change does not trigger stream restart" {
        val client = createLedStripClientEntity(
            clientRepository,
            "Test Client",
            "192.168.1.100",
            8000,
            8001
        )

        val updateRequest = UpdateClientRequest(
            name = "New Name",
            address = null,
            colorOrder = null,
            apiPort = null,
            wsPort = null,
            powerLimit = null
        )

        ledClientApiService.updateClient(client.uuid!!, updateRequest)

        val streamJobManager = getMock(streamJobManager)
        verify(exactly = 0) { streamJobManager.stopWebsocketJob(any()) }
        verify(exactly = 0) { streamJobManager.startStreamingJob(any()) }

        val updatedClient = clientRepository.findByUuid(client.uuid!!).get()
        updatedClient.name shouldBe updateRequest.name
    }
}) {
    @MockBean(StreamJobManager::class)
    fun streamJobManager(): StreamJobManager {
        return mockk(relaxed = true)
    }
}
