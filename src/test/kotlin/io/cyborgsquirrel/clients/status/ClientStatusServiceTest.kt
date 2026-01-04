package io.cyborgsquirrel.clients.status

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientStatus
import io.cyborgsquirrel.jobs.streaming.StreamJobManager
import io.cyborgsquirrel.jobs.streaming.StreamingJobState
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.service.ActiveLightEffectService
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.kotest5.MicronautKotest5Extension.getMock
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import java.util.*

@MicronautTest(startApplication = false, transactional = false)
class ClientStatusServiceTest(
    private val activeLightEffectService: ActiveLightEffectService,
    private val jobsManager: StreamJobManager,
    private val service: ClientStatusService
) : StringSpec({
    lateinit var mockActiveLightEffectService: ActiveLightEffectService
    lateinit var mockJobsManager: StreamJobManager

    beforeTest {
        mockActiveLightEffectService = getMock(activeLightEffectService)
        mockJobsManager = getMock(jobsManager)
    }

    "Disconnected status" {
        every {
            mockJobsManager.getJobState(any())
        } returns StreamingJobState.Offline
        val mockClientEntity = mockk<LedStripClientEntity>()
        var statusInfoOptional = service.getStatusForClient(mockClientEntity)

        statusInfoOptional.isPresent shouldBe true
        var statusInfo = statusInfoOptional.get()
        statusInfo.status shouldBe ClientStatus.Offline

        every {
            mockJobsManager.getJobState(any())
        } returns StreamingJobState.WaitingForConnection

        statusInfoOptional = service.getStatusForClient(mockClientEntity)

        statusInfoOptional.isPresent shouldBe true
        statusInfo = statusInfoOptional.get()
        statusInfo.status shouldBe ClientStatus.Offline
    }

    "Idle status" {
        every {
            mockJobsManager.getJobState(any())
        } returns StreamingJobState.ConnectedIdle
        every {
            mockActiveLightEffectService.getAllEffectsForStrip(any())
        } returns listOf()

        val mockClientEntity = mockk<LedStripClientEntity>()
        val mockStrip = mockk<LedStripEntity>()
        val stripUuid = UUID.randomUUID().toString()

        every {
            mockStrip.uuid
        } returns stripUuid
        every {
            mockClientEntity.strips
        } returns setOf(mockStrip)
        every {
            activeLightEffectService.getAllEffectsForStrip(stripUuid)
        } returns listOf()

        val statusInfoOptional = service.getStatusForClient(mockClientEntity)

        statusInfoOptional.isPresent shouldBe true
        val statusInfo = statusInfoOptional.get()
        statusInfo.status shouldBe ClientStatus.Idle
        statusInfo.activeEffects shouldBe 0
    }

    "Setup incomplete status" {
        every {
            mockJobsManager.getJobState(any())
        } returns StreamingJobState.SetupIncomplete
        every {
            mockActiveLightEffectService.getAllEffectsForStrip(any())
        } returns listOf()

        val mockClientEntity = mockk<LedStripClientEntity>()
        val statusInfoOptional = service.getStatusForClient(mockClientEntity)

        statusInfoOptional.isPresent shouldBe true
        val statusInfo = statusInfoOptional.get()
        statusInfo.status shouldBe ClientStatus.SetupIncomplete
        statusInfo.activeEffects shouldBe 0
    }

    "Active status" {
        every {
            mockJobsManager.getJobState(any())
        } returns StreamingJobState.RenderingEffect
        every {
            mockActiveLightEffectService.getAllEffectsForStrip(any())
        } returns listOf()

        val mockClientEntity = mockk<LedStripClientEntity>()
        val mockStrip = mockk<LedStripEntity>()
        val stripUuid = UUID.randomUUID().toString()
        val activeEffect = mockk<ActiveLightEffect>()

        every {
            mockStrip.uuid
        } returns stripUuid
        every {
            mockClientEntity.strips
        } returns setOf(mockStrip)
        every {
            activeLightEffectService.getAllEffectsForStrip(stripUuid)
        } returns listOf(activeEffect)
        every {
            activeEffect.status
        } returns LightEffectStatus.Playing

        val statusInfoOptional = service.getStatusForClient(mockClientEntity)

        statusInfoOptional.isPresent shouldBe true
        val statusInfo = statusInfoOptional.get()
        statusInfo.status shouldBe ClientStatus.Active
        statusInfo.activeEffects shouldBe 1
    }
}) {
    @MockBean(ActiveLightEffectService::class)
    fun activeLightEffectRegistry(): ActiveLightEffectService {
        return mockk()
    }

    @MockBean(StreamJobManager::class)
    fun jobsManager(): StreamJobManager {
        return mockk()
    }
}