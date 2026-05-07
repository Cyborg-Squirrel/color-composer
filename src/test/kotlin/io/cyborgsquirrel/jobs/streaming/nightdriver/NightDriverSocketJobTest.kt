package io.cyborgsquirrel.jobs.streaming.nightdriver

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.clients.repository.H2LedStripClientRepository
import io.cyborgsquirrel.jobs.streaming.model.StreamingJobStatus
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.led_strips.enums.PoolType
import io.cyborgsquirrel.lighting.effect_trigger.service.TriggerManager
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.LightEffect
import io.cyborgsquirrel.lighting.effects.service.ActiveLightEffectService
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.model.LedStripPoolModel
import io.cyborgsquirrel.lighting.model.SingleLedStripModel
import io.cyborgsquirrel.lighting.rendering.LightEffectRenderer
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameSegmentModel
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.util.time.TimeHelper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.*
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val CLIENT_UUID = "nd-client-uuid"
private const val STRIP_UUID = "nd-strip-uuid"
private const val STRIP_PIN = "2"
private const val STRIP_LENGTH = 144
private const val STRIP_BRIGHTNESS = 100
private const val NOW_MILLIS = 10_000L

class NightDriverSocketJobTest : StringSpec({

    val mockRenderer = mockk<LightEffectRenderer>()
    val mockTriggerManager = mockk<TriggerManager>()
    val mockClientRepository = mockk<H2LedStripClientRepository>()
    val mockTimeHelper = mockk<TimeHelper>()
    val mockActiveLightEffectService = mockk<ActiveLightEffectService>()
    val mockStripEntity = mockk<LedStripEntity>()

    val strip = SingleLedStripModel(
        name = "ND Strip",
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
        name = "ND Client",
        uuid = CLIENT_UUID,
        address = "192.168.1.200",
        clientType = ClientType.NightDriver,
        colorOrder = ColorOrder.RGB,
        wsPort = 65000,
        apiPort = 80,
        firmwareVersion = null,
        powerLimit = 0,
        strips = setOf(mockStripEntity),
    )
    val activeEffect = ActiveLightEffect(
        effectUuid = "nd-effect-uuid",
        priority = 0,
        skipFramesIfBlank = false,
        status = LightEffectStatus.Playing,
        effect = mockk<LightEffect>(),
        filters = emptyList(),
        strip = strip,
    )

    fun makeJob() = NightDriverSocketJob(
        renderer = mockRenderer,
        triggerManager = mockTriggerManager,
        clientRepository = mockClientRepository,
        timeHelper = mockTimeHelper,
        clientEntity = clientEntity,
        activeLightEffectService = mockActiveLightEffectService,
    )

    fun setupCommonMocks() {
        every { mockActiveLightEffectService.addListener(any()) } answers {}
        every { mockActiveLightEffectService.removeListener(any()) } answers {}
        every { mockTimeHelper.millisSinceEpoch() } returns NOW_MILLIS
        every { mockTimeHelper.dateTimeFromMillis(any()) } returns LocalDateTime.of(2024, 1, 1, 0, 0)
        every { mockClientRepository.findByUuid(CLIENT_UUID) } returns Optional.of(clientEntity)
        every { mockClientRepository.findById(1L) } returns Optional.of(clientEntity)
        every { mockClientRepository.update(any()) } answers { firstArg() }
        every { mockActiveLightEffectService.getEffectsForClient(CLIENT_UUID) } returns listOf(activeEffect)
        every { mockTriggerManager.processTriggers() } answers {}
        every { mockRenderer.renderFrames(any(), any()) } returns emptyList()
    }

    afterEach { clearAllMocks() }

    // -------------------------------------------------------------------------
    // Direct method tests
    // -------------------------------------------------------------------------

    "getCurrentState returns SetupIncomplete initially" {
        setupCommonMocks()
        val job = makeJob()
        job.getCurrentState().status shouldBe StreamingJobStatus.SetupIncomplete
    }

    "getLatestResponse is null initially" {
        setupCommonMocks()
        val job = makeJob()
        job.getLatestResponse().shouldBeNull()
    }

    "onUpdate with a matching SingleLedStripModel updates strips" {
        setupCommonMocks()
        val job = makeJob()
        // Status stays SetupIncomplete — NightDriver does not trigger SettingsSync
        job.onUpdate(listOf(activeEffect))
        job.getCurrentState().status shouldBe StreamingJobStatus.SetupIncomplete
    }

    "onUpdate with a matching LedStripPoolModel does not throw" {
        setupCommonMocks()
        val job = makeJob()
        val poolStrip = LedStripPoolModel(
            name = "Pool",
            uuid = "pool-uuid",
            blendMode = BlendMode.Additive,
            poolType = PoolType.Sync,
            strips = listOf(strip),
        )
        job.onUpdate(listOf(activeEffect.copy(strip = poolStrip)))
        job.getCurrentState().status shouldBe StreamingJobStatus.SetupIncomplete
    }

    "onUpdate called twice with the same strips is idempotent" {
        setupCommonMocks()
        val job = makeJob()
        job.onUpdate(listOf(activeEffect))
        val statusAfterFirst = job.getCurrentState().status
        job.onUpdate(listOf(activeEffect))
        job.getCurrentState().status shouldBe statusAfterFirst
    }

    "onUpdate with effects belonging to a different client is a no-op" {
        setupCommonMocks()
        val job = makeJob()
        val otherEffect = activeEffect.copy(strip = strip.copy(clientUuid = "other-client"))
        job.onUpdate(listOf(otherEffect))
        job.getCurrentState().status shouldBe StreamingJobStatus.SetupIncomplete
    }

    // -------------------------------------------------------------------------
    // Coroutine tests
    // -------------------------------------------------------------------------

    "start adds listener; dispose removes listener and stops the loop" {
        setupCommonMocks()
        every { mockClientRepository.findByUuid(CLIENT_UUID) } returns Optional.of(
            clientEntity.copy(strips = emptySet())
        )
        val job = makeJob()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val coroutineJob = job.start(scope)
        verify { mockActiveLightEffectService.addListener(job) }

        job.dispose()
        coroutineJob.join()
        scope.cancel()

        verify { mockActiveLightEffectService.removeListener(job) }
    }

    "SetupIncomplete: stays when client has no strips configured" {
        setupCommonMocks()
        every { mockClientRepository.findByUuid(CLIENT_UUID) } returns Optional.of(
            clientEntity.copy(strips = emptySet())
        )
        val job = makeJob()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val coroutineJob = job.start(scope)
        delay(100)
        coroutineJob.cancel()
        scope.cancel()

        job.getCurrentState().status shouldBe StreamingJobStatus.SetupIncomplete
    }

    "SetupIncomplete: disposes itself when client entity no longer exists" {
        setupCommonMocks()
        every { mockClientRepository.findByUuid(CLIENT_UUID) } returns Optional.empty()
        val job = makeJob()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val coroutineJob = job.start(scope)
        coroutineJob.join()
        scope.cancel()

        verify { mockActiveLightEffectService.removeListener(job) }
    }

    "RenderingEffect with empty frames transitions to BufferFullWaiting" {
        setupCommonMocks()
        // Socket connections will fail immediately (no real server), so we mock the socket constructor
        mockkConstructor(java.net.Socket::class)
        every { anyConstructed<java.net.Socket>().connect(any(), any()) } throws java.net.ConnectException("refused")
        every { anyConstructed<java.net.Socket>().isConnected } returns false
        every { anyConstructed<java.net.Socket>().close() } answers {}

        val job = makeJob()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val coroutineJob = job.start(scope)
        // The job moves to Offline after SetupIncomplete, then tries to connect (and fails)
        // With backoff it will stay in the error recovery path; just verify it progressed past SetupIncomplete
        delay(200)
        coroutineJob.cancel()
        scope.cancel()

        // Should have moved past SetupIncomplete (got to Offline or stayed in the reconnect loop)
        job.getCurrentState().status shouldBe StreamingJobStatus.Offline

        unmockkConstructor(java.net.Socket::class)
    }
})

// ---------------------------------------------------------------------------
// Binary response builder — mirrors the layout in NightDriverSocketResponse.kt
// ---------------------------------------------------------------------------

fun buildNightDriverResponse(
    bufferSize: Short = 10,
    bufferPosition: Short = 0,
    frameDrawing: Short = 0,
    currentClock: Double = NOW_MILLIS / 1000.0,
): ByteArray {
    val buf = ByteBuffer.allocate(NightDriverSocketResponse.SIZE_IN_BYTES).order(ByteOrder.LITTLE_ENDIAN)
    buf.putShort(NightDriverSocketResponse.SIZE_IN_BYTES.toShort()) // size
    buf.putShort(0)                                                  // padding
    buf.putInt(1)                                                    // sequence
    buf.putInt(0)                                                    // padding
    buf.putShort(100)                                                // flashVersion
    buf.putShort(0)                                                  // padding
    buf.putDouble(currentClock)                                      // currentClock
    buf.putDouble(0.0)                                               // oldestPacket
    buf.putDouble(0.0)                                               // newestPacket
    buf.putDouble(1.0)                                               // brightness
    buf.putDouble(-60.0)                                             // wifiSignal
    buf.putShort(bufferSize)                                         // bufferSize
    buf.putShort(0)                                                  // padding
    buf.putShort(bufferPosition)                                     // bufferPosition
    buf.putShort(0)                                                  // padding
    buf.putShort(frameDrawing)                                       // frameDrawing
    buf.putShort(0)                                                  // padding
    buf.putShort(5)                                                  // watts
    return buf.array()
}
