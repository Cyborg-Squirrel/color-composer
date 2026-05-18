package io.cyborgsquirrel.jobs.streaming.pi_client

import io.cyborgsquirrel.clients.config.pi_client.PiClientSettings
import io.cyborgsquirrel.clients.config.pi_client.PiClientStripConfig
import io.cyborgsquirrel.clients.config.pi_client.PiClientStripsConfigList
import io.cyborgsquirrel.clients.config.pi_client.PiConfigClient
import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.clients.model.ClientTime
import io.cyborgsquirrel.clients.model.ClientVersion
import io.cyborgsquirrel.clients.repository.LedStripClientRepository
import io.cyborgsquirrel.jobs.streaming.model.StreamingJobStatus
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.lighting.effect_trigger.service.TriggerManager
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.LightEffect
import io.cyborgsquirrel.lighting.effects.service.LightEffectRegistry
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.model.LedStripPoolModel
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.SingleLedStripModel
import io.cyborgsquirrel.lighting.rendering.LightEffectRenderer
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameSegmentModel
import io.cyborgsquirrel.util.time.TimeHelper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.websocket.WebSocketClient
import io.mockk.*
import kotlinx.coroutines.*
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import org.reactivestreams.Subscription
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue

private const val CLIENT_UUID = "test-client-uuid"
private const val STRIP_UUID = "test-strip-uuid"
private const val STRIP_PIN = "D10"
private const val STRIP_LENGTH = 60
private const val STRIP_BRIGHTNESS = 100
private const val NOW_MILLIS = 10_000L

class PiClientWebSocketJobTest : StringSpec({

    val mockWebSocketClient = mockk<WebSocketClient>()
    val mockRenderer = mockk<LightEffectRenderer>()
    val mockTriggerManager = mockk<TriggerManager>()
    val mockClientRepository = mockk<LedStripClientRepository>()
    val mockTimeHelper = mockk<TimeHelper>()
    val mockPiConfigClient = mockk<PiConfigClient>()
    val mockLightEffectRegistry = mockk<LightEffectRegistry>()
    val mockPiWebSocketClient = mockk<PiWebSocketClient>()
    val mockStripEntity = mockk<LedStripEntity>()

    val strip = SingleLedStripModel(
        name = "Test Strip",
        uuid = STRIP_UUID,
        pin = STRIP_PIN,
        length = STRIP_LENGTH,
        height = 1,
        blendMode = BlendMode.Additive,
        brightness = STRIP_BRIGHTNESS,
        clientUuid = CLIENT_UUID,
        inverted = false,
    )
    val clientEntity = LedStripClientEntity(
        id = 1L,
        name = "Test Client",
        uuid = CLIENT_UUID,
        address = "192.168.1.100",
        clientType = ClientType.Pi,
        colorOrder = ColorOrder.RGB,
        wsPort = 8765,
        apiPort = 8080,
        firmwareVersion = "0.1.0",
        powerLimit = 0,
        strips = setOf(mockStripEntity),
        fps = 35,
        fadeTimeoutMillis = 15000,
    )
    val activeEffect = ActiveLightEffect(
        effectUuid = "effect-uuid",
        priority = 0,
        skipFramesIfBlank = false,
        status = LightEffectStatus.Playing,
        effect = mockk<LightEffect>(),
        filters = emptyList(),
        strip = strip,
    )

    // A publisher that synchronously delivers the client inside request()
    fun immediatePublisher(piClient: PiWebSocketClient): Publisher<PiWebSocketClient> =
        Publisher { subscriber ->
            subscriber.onSubscribe(object : Subscription {
                override fun request(n: Long) { subscriber.onNext(piClient) }
                override fun cancel() {}
            })
        }

    fun makeJob() = PiClientWebSocketJob(
        webSocketClient = mockWebSocketClient,
        renderer = mockRenderer,
        triggerManager = mockTriggerManager,
        clientRepository = mockClientRepository,
        timeHelper = mockTimeHelper,
        piConfigClient = mockPiConfigClient,
        clientEntity = clientEntity,
        activeLightEffectService = mockLightEffectRegistry,
    )

    var mockResponseQueue = ConcurrentLinkedQueue<ByteArray>()

    fun setupCommonMocks() {
        mockResponseQueue = ConcurrentLinkedQueue()
        every { mockLightEffectRegistry.updates } returns Flux.never()
        every { mockTimeHelper.millisSinceEpoch() } returns NOW_MILLIS
        every { mockTimeHelper.dateTimeFromMillis(any()) } returns LocalDateTime.of(2024, 1, 1, 0, 0)
        every { mockClientRepository.findByUuid(CLIENT_UUID) } returns Optional.of(clientEntity)
        every { mockClientRepository.findById(1L) } returns Optional.of(clientEntity)
        every { mockClientRepository.update(any()) } answers { firstArg() }
        every { mockLightEffectRegistry.getEffectsForClient(CLIENT_UUID) } returns listOf(activeEffect)
        every { mockTriggerManager.processTriggers() } answers {}
        every { mockRenderer.renderFrames(any(), any()) } returns emptyList()
        every { mockPiWebSocketClient.registerOnDisconnectedCallback(any()) } answers {}
        every { mockPiWebSocketClient.unregisterOnDisconnectedCallback() } answers {}
        every { mockPiWebSocketClient.close() } answers {}
        every { mockPiWebSocketClient.responseQueue } returns mockResponseQueue
        every { mockPiWebSocketClient.send(any()) } returns CompletableFuture.completedFuture(byteArrayOf())
        // Use answers {} and explicit types to resolve the generic connect() overloads
        every { mockWebSocketClient.connect(any<Class<PiWebSocketClient>>(), any<java.net.URI>()) } answers {
            immediatePublisher(mockPiWebSocketClient)
        }
        coEvery { mockPiConfigClient.getStripConfigs(any()) } returns PiClientStripsConfigList(
            listOf(PiClientStripConfig(STRIP_UUID, STRIP_PIN, STRIP_LENGTH, STRIP_BRIGHTNESS, ColorOrder.RGB))
        )
        coEvery { mockPiConfigClient.getClientSettings(any()) } returns PiClientSettings(500, 15000)
        coEvery { mockPiConfigClient.getClientVersion(any()) } returns ClientVersion("1.0.0")
        coEvery { mockPiConfigClient.getClientTime(any()) } returns ClientTime(NOW_MILLIS)
        coEvery { mockPiConfigClient.updateClientSettings(any(), any()) } answers {}
    }

    afterEach { clearAllMocks() }

    // -------------------------------------------------------------------------
    // Direct method tests — no coroutine loop required
    // -------------------------------------------------------------------------

    "getCurrentState returns the initial SetupIncomplete status" {
        setupCommonMocks()
        val job = makeJob()
        job.getCurrentState().status shouldBe StreamingJobStatus.SetupIncomplete
    }

    "onUpdate with a matching SingleLedStripModel sets SettingsSync" {
        setupCommonMocks()
        val job = makeJob()
        job.onEffectsUpdate(listOf(activeEffect))
        job.getCurrentState().status shouldBe StreamingJobStatus.SettingsSync
    }

    "onUpdate with a matching LedStripPoolModel sets SettingsSync" {
        setupCommonMocks()
        val job = makeJob()
        val poolStrip = LedStripPoolModel(
            name = "Pool",
            uuid = "pool-uuid",
            blendMode = BlendMode.Additive,
            poolType = PoolType.Sync,
            strips = listOf(strip),
        )
        job.onEffectsUpdate(listOf(activeEffect.copy(strip = poolStrip)))
        job.getCurrentState().status shouldBe StreamingJobStatus.SettingsSync
    }

    "onUpdate called twice with the same strips does not change status a second time" {
        setupCommonMocks()
        val job = makeJob()
        job.onEffectsUpdate(listOf(activeEffect))
        val statusAfterFirst = job.getCurrentState().status
        job.onEffectsUpdate(listOf(activeEffect))
        job.getCurrentState().status shouldBe statusAfterFirst
    }

    "onUpdate with effects belonging to a different client is a no-op" {
        setupCommonMocks()
        val job = makeJob()
        val otherEffect = activeEffect.copy(strip = strip.copy(clientUuid = "other-client"))
        job.onEffectsUpdate(listOf(otherEffect))
        job.getCurrentState().status shouldBe StreamingJobStatus.SetupIncomplete
    }

    // -------------------------------------------------------------------------
    // Coroutine tests — use a real CoroutineScope + short delays
    // -------------------------------------------------------------------------

    "start adds listener; dispose removes listener and stops the loop" {
        setupCommonMocks()
        // Keep the job in SetupIncomplete so the loop polls slowly
        every { mockClientRepository.findByUuid(CLIENT_UUID) } returns Optional.of(
            clientEntity.copy(strips = emptySet())
        )
        val job = makeJob()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val coroutineJob = job.start(scope)
        verify { mockLightEffectRegistry.updates }

        job.dispose()
        coroutineJob.join()
        scope.cancel()
    }

    "SetupIncomplete: stays in SetupIncomplete when client has no strips configured" {
        setupCommonMocks()
        every { mockClientRepository.findByUuid(CLIENT_UUID) } returns Optional.of(
            clientEntity.copy(strips = emptySet())
        )
        val job = makeJob()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val coroutineJob = job.start(scope)
        delay(50) // well before the 5 s polling delay
        coroutineJob.cancel()
        scope.cancel()

        job.getCurrentState().status shouldBe StreamingJobStatus.SetupIncomplete
    }

    "SetupIncomplete: disposes itself when the client entity no longer exists" {
        setupCommonMocks()
        every { mockClientRepository.findByUuid(CLIENT_UUID) } returns Optional.empty()
        val job = makeJob()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val coroutineJob = job.start(scope)
        coroutineJob.join() // dispose() sets shouldRun=false so the loop exits naturally
        scope.cancel()
    }

    "happy path: transitions from SetupIncomplete all the way to TimeSyncRequired" {
        setupCommonMocks()
        val job = makeJob()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val coroutineJob = job.start(scope)
        // All mocked calls return instantly; 200 ms is enough for the state machine to
        // progress through SetupIncomplete → ConnectedIdle → SettingsSync → TimeSyncRequired
        delay(200)
        coroutineJob.cancel()
        scope.cancel()

        job.getCurrentState().status shouldBe StreamingJobStatus.TimeSyncRequired
    }

    "handleResponse: BackpressureError transitions to BufferFullWaiting" {
        setupCommonMocks()
        // Simulate the Pi immediately echoing a backpressure response on each send
        every { mockPiWebSocketClient.send(any()) } answers {
            mockResponseQueue.add(buildBackpressureResponse())
            CompletableFuture.completedFuture(byteArrayOf())
        }
        every { mockRenderer.renderFrames(any(), any()) } returns listOf(
            RenderedFrameSegmentModel(strip, 0, listOf(RgbColor.Red))
        )
        val job = makeJob()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val coroutineJob = job.start(scope)
        delay(200)
        coroutineJob.cancel()
        scope.cancel()

        job.getCurrentState().status shouldBe StreamingJobStatus.BufferFullWaiting
    }

    "handleResponse: GenericError transitions to Offline and closes the client" {
        setupCommonMocks()
        every { mockPiWebSocketClient.send(any()) } answers {
            mockResponseQueue.add(buildGenericErrorResponse("generic error"))
            CompletableFuture.completedFuture(byteArrayOf())
        }
        every { mockRenderer.renderFrames(any(), any()) } returns listOf(
            RenderedFrameSegmentModel(strip, 0, listOf(RgbColor.Red))
        )
        val job = makeJob()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val coroutineJob = job.start(scope)
        delay(50)
        coroutineJob.cancel()
        scope.cancel()

        job.getCurrentState().status shouldBe StreamingJobStatus.Offline
        verify { mockPiWebSocketClient.close() }
    }
})

// ---------------------------------------------------------------------------
// Binary response helpers — mirror the format in PiClientResponse.toPiClientResponse()
// ---------------------------------------------------------------------------

private fun buildMessage(type: Int, body: ByteArray): ByteArray {
    val buf = java.nio.ByteBuffer.allocate(2 + body.size).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    buf.put(type.toByte())
    buf.put(body.size.toByte())
    buf.put(body)
    return buf.array()
}

private fun buildBackpressureResponse() =
    buildMessage(1, "Backpressure".toByteArray(Charsets.UTF_8))

private fun buildGenericErrorResponse(message: String) =
    buildMessage(2, message.toByteArray(Charsets.UTF_8))
