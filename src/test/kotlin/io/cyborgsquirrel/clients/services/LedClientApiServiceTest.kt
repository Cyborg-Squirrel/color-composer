package io.cyborgsquirrel.clients.services

import io.cyborgsquirrel.clients.enums.ClientStatus
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.clients.requests.CreateClientRequest
import io.cyborgsquirrel.clients.requests.UpdateClientRequest
import io.cyborgsquirrel.clients.responses.GetClientResponse
import io.cyborgsquirrel.clients.status.ClientStatusInfo
import io.cyborgsquirrel.clients.status.ClientStatusService
import io.cyborgsquirrel.event_source.model.LedClientEvent
import io.cyborgsquirrel.event_source.model.SseEvent
import io.cyborgsquirrel.event_source.model.delta.ClientDelta
import io.cyborgsquirrel.event_source.service.SseEventEmitter
import io.cyborgsquirrel.jobs.streaming.StreamJobManager
import io.cyborgsquirrel.led_strips.repository.LedStripRepository
import io.cyborgsquirrel.test_helpers.createLedStripClientEntity
import io.cyborgsquirrel.test_helpers.saveLedStrip
import io.cyborgsquirrel.util.exception.ClientRequestException
import io.cyborgsquirrel.util.exception.ResourceNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.kotest5.MicronautKotest5Extension.getMock
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.Disposable
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

@MicronautTest
class LedClientApiServiceTest(
    private val ledClientApiService: LedClientApiService,
    private val clientRepository: LedStripClientRepository,
    private val stripRepository: LedStripRepository,
    private val streamJobManager: StreamJobManager,
    private val clientStatusService: ClientStatusService,
    private val sseEventEmitter: SseEventEmitter,
) : StringSpec({

    lateinit var mockStreamJobManager: StreamJobManager
    lateinit var mockClientStatusService: ClientStatusService

    // SseEventEmitter is a final concrete bean (not proxyable), so it can't be a @MockBean. Instead we
    // subscribe to its event stream and collect everything emitted. Emission is synchronous, so the list is
    // fully populated by the time the service call returns. The subscription lives for the whole spec: the
    // multicast sink auto-cancels once it has no subscribers, so disposing per-test would kill it for the
    // following tests. We clear the list before each test instead.
    val emittedEvents = CopyOnWriteArrayList<SseEvent>()
    var eventSubscription: Disposable? = null

    beforeSpec {
        eventSubscription = sseEventEmitter.events.subscribe { emittedEvents.add(it) }
    }

    afterSpec {
        eventSubscription?.dispose()
    }

    beforeTest {
        mockStreamJobManager = getMock(streamJobManager)
        mockClientStatusService = getMock(clientStatusService)

        // Sensible default so response mapping has a deterministic status; individual tests override as needed.
        every { mockClientStatusService.getStatusForClient(any()) } returns
                Optional.of(ClientStatusInfo.inactive(ClientStatus.Idle))

        emittedEvents.clear()
    }

    afterTest {
        stripRepository.deleteAll()
        clientRepository.deleteAll()
    }

    "getAllClients returns empty response when no clients exist" {
        val response = ledClientApiService.getAllClients()
        response.clients.isEmpty() shouldBe true
    }

    "getAllClients returns every persisted client mapped to a response" {
        createLedStripClientEntity(clientRepository, "Client A", "192.168.1.10", 8000, 8001)
        createLedStripClientEntity(clientRepository, "Client B", "192.168.1.11", 8002, 8003)
        createLedStripClientEntity(clientRepository, "Client C", "192.168.1.12", 8004, 8005)

        val response = ledClientApiService.getAllClients()

        response.clients.size shouldBe 3
        response.clients.map { it.name }.toSet() shouldBe setOf("Client A", "Client B", "Client C")
        response.clients.map { it.address }.toSet() shouldBe setOf("192.168.1.10", "192.168.1.11", "192.168.1.12")
    }

    "getClientWithUuid returns the matching client mapped with all fields" {
        val client = createLedStripClientEntity(
            clientRepository, "Test Client", "192.168.1.100", 8000, 8001, powerLimit = 250
        )
        every { mockClientStatusService.getStatusForClient(any()) } returns
                Optional.of(ClientStatusInfo(ClientStatus.Active, 4))

        val response = ledClientApiService.getClientWithUuid(client.uuid)

        response.uuid shouldBe client.uuid
        response.name shouldBe client.name
        response.address shouldBe client.address
        response.clientType shouldBe client.clientType.toString()
        response.colorOrder shouldBe client.colorOrder
        response.apiPort shouldBe client.apiPort
        response.wsPort shouldBe client.wsPort
        response.lastSeenAt shouldBe client.lastSeenAt
        response.powerLimit shouldBe client.powerLimit
        response.firmwareVersion shouldBe client.firmwareVersion
        response.fps shouldBe client.fps
        response.fadeTimeoutMillis shouldBe client.fadeTimeoutMillis
        response.status shouldBe ClientStatus.Active
        response.activeEffects shouldBe 4
    }

    "getClientWithUuid throws ResourceNotFoundException when the client does not exist" {
        shouldThrow<ResourceNotFoundException> {
            ledClientApiService.getClientWithUuid(UUID.randomUUID().toString())
        }
    }

    "createClient persists the client, starts streaming, emits a created event and returns the uuid" {
        val request = CreateClientRequest(
            name = "New Client",
            address = "192.168.1.50",
            clientType = ClientType.Pi,
            colorOrder = ColorOrder.GRB,
            apiPort = 9000,
            wsPort = 9001,
            powerLimit = 500,
            fps = 60,
            fadeTimeoutMillis = 1000,
        )

        val uuid = ledClientApiService.createClient(request)

        val saved = clientRepository.findByUuid(uuid)
        saved.isPresent shouldBe true
        val entity = saved.get()
        entity.name shouldBe request.name
        entity.address shouldBe request.address
        entity.clientType shouldBe request.clientType
        entity.colorOrder shouldBe ColorOrder.GRB
        entity.apiPort shouldBe request.apiPort
        entity.wsPort shouldBe request.wsPort
        entity.powerLimit shouldBe request.powerLimit
        entity.fps shouldBe request.fps
        entity.fadeTimeoutMillis shouldBe request.fadeTimeoutMillis
        entity.firmwareVersion shouldBe LedStripClientEntity.DEFAULT_FIRMWARE_VERSION

        verify(exactly = 1) { mockStreamJobManager.startStreamingJob(any()) }

        emittedEvents.size shouldBe 1
        val event = emittedEvents.first()
        event.shouldBeInstanceOf<LedClientEvent.LedClientCreated>()
        event.uuid shouldBe uuid
        val data = event.data
        data.shouldBeInstanceOf<GetClientResponse>()
        data.name shouldBe request.name
        data.address shouldBe request.address
    }

    "createClient defaults colorOrder to RGB when not provided" {
        val request = CreateClientRequest(
            name = "Pi Client",
            address = "192.168.1.51",
            clientType = ClientType.Pi,
            colorOrder = null,
            apiPort = 9000,
            wsPort = 9001,
            powerLimit = 100,
        )

        val uuid = ledClientApiService.createClient(request)

        clientRepository.findByUuid(uuid).get().colorOrder shouldBe ColorOrder.RGB
    }

    "createClient defaults powerLimit, fps and fadeTimeout when not provided" {
        val request = CreateClientRequest(
            name = "Defaults Client",
            address = "192.168.1.52",
            clientType = ClientType.Pi,
            colorOrder = ColorOrder.RGB,
            apiPort = 9000,
            wsPort = 9001,
            powerLimit = null,
            fps = null,
            fadeTimeoutMillis = null,
        )

        val uuid = ledClientApiService.createClient(request)

        val entity = clientRepository.findByUuid(uuid).get()
        entity.powerLimit shouldBe 0
        entity.fps shouldBe LedStripClientEntity.DEFAULT_FPS
        entity.fadeTimeoutMillis shouldBe LedStripClientEntity.DEFAULT_FADE_TIMEOUT_MILLIS
    }

    "createClient throws ClientRequestException when a client with the same address exists" {
        createLedStripClientEntity(clientRepository, "Existing", "192.168.1.99", 8000, 8001)

        val request = CreateClientRequest(
            name = "Duplicate",
            address = "192.168.1.99",
            clientType = ClientType.Pi,
            colorOrder = ColorOrder.RGB,
            apiPort = 9000,
            wsPort = 9001,
            powerLimit = null,
        )

        shouldThrow<ClientRequestException> {
            ledClientApiService.createClient(request)
        }

        // No side effects should have happened on the conflict path.
        verify(exactly = 0) { mockStreamJobManager.startStreamingJob(any()) }
        emittedEvents.isEmpty() shouldBe true
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

        ledClientApiService.updateClient(client.uuid, updateRequest)

        verify(atLeast = 1) { mockStreamJobManager.stopWebsocketJob(any()) }
        verify(atLeast = 1) { mockStreamJobManager.startStreamingJob(any()) }

        val updatedClient = clientRepository.findByUuid(client.uuid).get()
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

        ledClientApiService.updateClient(client.uuid, updateRequest)

        verify(atLeast = 1) { mockStreamJobManager.stopWebsocketJob(any()) }
        verify(atLeast = 1) { mockStreamJobManager.startStreamingJob(any()) }

        val updatedClient = clientRepository.findByUuid(client.uuid).get()
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

        ledClientApiService.updateClient(client.uuid, updateRequest)

        verify(atLeast = 1) { mockStreamJobManager.stopWebsocketJob(any()) }
        verify(atLeast = 1) { mockStreamJobManager.startStreamingJob(any()) }

        val updatedClient = clientRepository.findByUuid(client.uuid).get()
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

        ledClientApiService.updateClient(client.uuid, updateRequest)

        verify(atLeast = 1) { mockStreamJobManager.stopWebsocketJob(any()) }
        verify(atLeast = 1) { mockStreamJobManager.startStreamingJob(any()) }

        val updatedClient = clientRepository.findByUuid(client.uuid).get()
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

        ledClientApiService.updateClient(client.uuid, updateRequest)

        verify(atLeast = 1) { mockStreamJobManager.stopWebsocketJob(any()) }
        verify(atLeast = 1) { mockStreamJobManager.startStreamingJob(any()) }
    }

    "Update client with fps change triggers stream restart" {
        val client = createLedStripClientEntity(
            clientRepository, "Test Client", "192.168.1.100", 8000, 8001, fps = 30
        )

        val updateRequest = UpdateClientRequest(
            name = null,
            address = null,
            colorOrder = null,
            apiPort = null,
            wsPort = null,
            powerLimit = null,
            fps = 90  // Changed
        )

        ledClientApiService.updateClient(client.uuid, updateRequest)

        verify(atLeast = 1) { mockStreamJobManager.stopWebsocketJob(any()) }
        verify(atLeast = 1) { mockStreamJobManager.startStreamingJob(any()) }

        clientRepository.findByUuid(client.uuid).get().fps shouldBe 90
    }

    "Update client with fade timeout change triggers stream restart" {
        val client = createLedStripClientEntity(
            clientRepository, "Test Client", "192.168.1.100", 8000, 8001, fadeTimeoutMillis = 5000
        )

        val updateRequest = UpdateClientRequest(
            name = null,
            address = null,
            colorOrder = null,
            apiPort = null,
            wsPort = null,
            powerLimit = null,
            fadeTimeoutMillis = 20000  // Changed
        )

        ledClientApiService.updateClient(client.uuid, updateRequest)

        verify(atLeast = 1) { mockStreamJobManager.stopWebsocketJob(any()) }
        verify(atLeast = 1) { mockStreamJobManager.startStreamingJob(any()) }

        clientRepository.findByUuid(client.uuid).get().fadeTimeoutMillis shouldBe 20000
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

        ledClientApiService.updateClient(client.uuid, updateRequest)

        verify(exactly = 0) { mockStreamJobManager.stopWebsocketJob(any()) }
        verify(exactly = 0) { mockStreamJobManager.startStreamingJob(any()) }

        val updatedClient = clientRepository.findByUuid(client.uuid).get()
        updatedClient.name shouldBe updateRequest.name
    }

    "updateClient leaves fields unchanged when their request values are null" {
        val client = createLedStripClientEntity(
            clientRepository, "Original", "192.168.1.100", 8000, 8001, powerLimit = 75, fps = 40, fadeTimeoutMillis = 3000
        )
        val originalColorOrder = client.colorOrder

        val updateRequest = UpdateClientRequest(
            name = "Renamed",
            address = null,
            colorOrder = null,
            apiPort = null,
            wsPort = null,
            powerLimit = null,
        )

        ledClientApiService.updateClient(client.uuid, updateRequest)

        val updated = clientRepository.findByUuid(client.uuid).get()
        updated.name shouldBe "Renamed"
        updated.address shouldBe client.address
        updated.colorOrder shouldBe originalColorOrder
        updated.apiPort shouldBe client.apiPort
        updated.wsPort shouldBe client.wsPort
        updated.powerLimit shouldBe 75
        updated.fps shouldBe 40
        updated.fadeTimeoutMillis shouldBe 3000
    }

    "updateClient emits an update event whose delta only contains changed fields" {
        val client = createLedStripClientEntity(
            clientRepository, "Original", "192.168.1.100", 8000, 8001, powerLimit = 75
        )

        val updateRequest = UpdateClientRequest(
            name = "Renamed",
            address = null,
            colorOrder = null,
            apiPort = null,
            wsPort = null,
            powerLimit = null,
        )

        ledClientApiService.updateClient(client.uuid, updateRequest)

        emittedEvents.size shouldBe 1
        val event = emittedEvents.first()
        event.shouldBeInstanceOf<LedClientEvent.LedClientUpdated>()
        event.uuid shouldBe client.uuid
        val delta = event.data
        delta.shouldBeInstanceOf<ClientDelta>()
        delta.name shouldBe "Renamed"
        delta.address shouldBe null
        delta.colorOrder shouldBe null
        delta.apiPort shouldBe null
        delta.wsPort shouldBe null
        delta.powerLimit shouldBe null
        delta.fps shouldBe null
        delta.fadeTimeoutMillis shouldBe null
    }

    "updateClient throws ResourceNotFoundException when the client does not exist" {
        val updateRequest = UpdateClientRequest(
            name = "Whatever",
            address = null,
            colorOrder = null,
            apiPort = null,
            wsPort = null,
            powerLimit = null,
        )

        shouldThrow<ResourceNotFoundException> {
            ledClientApiService.updateClient(UUID.randomUUID().toString(), updateRequest)
        }

        verify(exactly = 0) { mockStreamJobManager.startStreamingJob(any()) }
        emittedEvents.isEmpty() shouldBe true
    }

    "deleteClient removes the client, stops its job and emits a deleted event when it has no strips" {
        val client = createLedStripClientEntity(clientRepository, "To Delete", "192.168.1.70", 8000, 8001)

        ledClientApiService.deleteClient(client.uuid)

        clientRepository.findByUuid(client.uuid).isPresent shouldBe false
        verify(exactly = 1) { mockStreamJobManager.stopWebsocketJob(any()) }

        emittedEvents.size shouldBe 1
        val event = emittedEvents.first()
        event.shouldBeInstanceOf<LedClientEvent.LedClientDeleted>()
        event.uuid shouldBe client.uuid
        event.data shouldBe null
    }

    "deleteClient throws ClientRequestException and keeps the client when it still has strips" {
        val client = createLedStripClientEntity(clientRepository, "Has Strips", "192.168.1.71", 8000, 8001)
        saveLedStrip(stripRepository, client, "Strip", 60, "D10", 100)

        shouldThrow<ClientRequestException> {
            ledClientApiService.deleteClient(client.uuid)
        }

        clientRepository.findByUuid(client.uuid).isPresent shouldBe true
        verify(exactly = 0) { mockStreamJobManager.stopWebsocketJob(any()) }
        emittedEvents.isEmpty() shouldBe true
    }

    "deleteClient throws ResourceNotFoundException when the client does not exist" {
        shouldThrow<ResourceNotFoundException> {
            ledClientApiService.deleteClient(UUID.randomUUID().toString())
        }

        verify(exactly = 0) { mockStreamJobManager.stopWebsocketJob(any()) }
        emittedEvents.isEmpty() shouldBe true
    }

    "mapClientEntityToResponse reflects the status and active effect count from the status service" {
        val client = createLedStripClientEntity(clientRepository, "Mapped", "192.168.1.80", 8000, 8001)
        every { mockClientStatusService.getStatusForClient(any()) } returns
                Optional.of(ClientStatusInfo(ClientStatus.Active, 7))

        val response = ledClientApiService.mapClientEntityToResponse(client)

        response.status shouldBe ClientStatus.Active
        response.activeEffects shouldBe 7
    }

    "mapClientEntityToResponse falls back to error status when the status service has none" {
        val client = createLedStripClientEntity(clientRepository, "No Status", "192.168.1.81", 8000, 8001)
        every { mockClientStatusService.getStatusForClient(any()) } returns Optional.empty()

        val response = ledClientApiService.mapClientEntityToResponse(client)

        response.status shouldBe ClientStatus.Error
        response.activeEffects shouldBe 0
    }
}) {
    @MockBean(StreamJobManager::class)
    fun streamJobManager(): StreamJobManager = mockk(relaxed = true)

    @MockBean(ClientStatusService::class)
    fun clientStatusService(): ClientStatusService = mockk(relaxed = true)
}
