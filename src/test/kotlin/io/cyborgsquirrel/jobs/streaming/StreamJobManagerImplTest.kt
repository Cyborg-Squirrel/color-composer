package io.cyborgsquirrel.jobs.streaming

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.jobs.streaming.model.NightDriverStreamingJobState
import io.cyborgsquirrel.jobs.streaming.model.PiStreamingJobState
import io.cyborgsquirrel.jobs.streaming.model.StreamingJobStatus
import io.cyborgsquirrel.jobs.streaming.nightdriver.NightDriverSocketResponse
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job

class StreamJobManagerImplTest : StringSpec({
    val mockStreamingJobFactory = mockk<StreamingJobFactory>()
    val mockStreamingJob = mockk<ClientStreamingJob>()
    val mockJob = mockk<Job>()
    val mockDisposable = mockk<DisposableHandle>()

    afterTest {
        clearMocks(mockStreamingJobFactory, mockStreamingJob, mockJob, mockDisposable)
    }
    
    fun setupCommonMocks(clientEntity: LedStripClientEntity, clientType: ClientType) {
        every { clientEntity.uuid } returns "test-uuid"
        every { clientEntity.clientType } returns clientType
        every { mockStreamingJobFactory.createJob(any()) } returns mockStreamingJob
        every { mockJob.invokeOnCompletion(any()) } returns mockDisposable
        every { mockStreamingJob.start(any()) } returns mockJob
        every { mockStreamingJob.getCurrentState() } returns PiStreamingJobState(StreamingJobStatus.SetupIncomplete)
        every { mockStreamingJob.dispose() } answers { }
    }
    
    "start a job for a new client" {
        val mockClientEntity = mockk<LedStripClientEntity>()
        setupCommonMocks(mockClientEntity, ClientType.Pi)
        
        val manager = StreamJobManagerImpl(mockStreamingJobFactory)

        manager.startStreamingJob(mockClientEntity)

        val jobState = manager.getJobState(mockClientEntity.uuid!!)
        jobState shouldBe PiStreamingJobState(StreamingJobStatus.SetupIncomplete)
        verify { mockStreamingJobFactory.createJob(mockClientEntity) }
        verify { mockStreamingJob.start(any()) }
    }

    "stop a websocket job" {
        val mockClientEntity = mockk<LedStripClientEntity>()
        setupCommonMocks(mockClientEntity, ClientType.Pi)
        coEvery { mockJob.cancel() } answers { }

        val manager = StreamJobManagerImpl(mockStreamingJobFactory)

        manager.startStreamingJob(mockClientEntity)
        manager.stopWebsocketJob(mockClientEntity)

        // Job state is null because it no longer exists
        val jobState = manager.getJobState(mockClientEntity.uuid!!)
        jobState shouldBe null
        verify { mockStreamingJob.dispose() }
        verify { mockJob.cancel() }
    }

    "handle multiple clients gracefully" {
        val mockClientEntityA = mockk<LedStripClientEntity>()
        val mockClientEntityB = mockk<LedStripClientEntity>()
        val mockStreamingJobA = mockk<ClientStreamingJob>()
        val mockStreamingJobB = mockk<ClientStreamingJob>()
        val mockJobA = mockk<Job>()
        val mockJobB = mockk<Job>()
        val mockDisposableA = mockk<DisposableHandle>()
        val mockDisposableB = mockk<DisposableHandle>()
        val expectedJobStateA = PiStreamingJobState(StreamingJobStatus.RenderingEffect)
        val expectedJobStateB = NightDriverStreamingJobState(
            StreamingJobStatus.RenderingEffect,
            NightDriverSocketResponse(72, 1, 40, 1.0, 2.0, 3.0, 4.0, -45.0, 2, 1, 35, 1)
        )

        every { mockClientEntityA.uuid } returns "test-uuid-1"
        every { mockClientEntityA.clientType } returns ClientType.Pi
        every { mockClientEntityB.uuid } returns "test-uuid-2"
        every { mockClientEntityB.clientType } returns ClientType.NightDriver
        every { mockStreamingJobFactory.createJob(mockClientEntityA) } returns mockStreamingJobA
        every { mockStreamingJobFactory.createJob(mockClientEntityB) } returns mockStreamingJobB
        every { mockJobA.invokeOnCompletion(any()) } returns mockDisposableA
        every { mockJobB.invokeOnCompletion(any()) } returns mockDisposableB
        every { mockStreamingJobA.start(any()) } returns mockJobA
        every { mockStreamingJobB.start(any()) } returns mockJobB
        every { mockStreamingJobA.getCurrentState() } returns expectedJobStateA
        every { mockStreamingJobB.getCurrentState() } returns expectedJobStateB
        every { mockStreamingJobA.dispose() } answers { }
        every { mockStreamingJobB.dispose() } answers { }

        val manager = StreamJobManagerImpl(mockStreamingJobFactory)

        manager.startStreamingJob(mockClientEntityA)
        manager.startStreamingJob(mockClientEntityB)

        val jobState1 = manager.getJobState(mockClientEntityA.uuid!!)
        val jobState2 = manager.getJobState(mockClientEntityB.uuid!!)
        jobState1 shouldBe expectedJobStateA
        jobState2 shouldBe expectedJobStateB
    }

    "handle job completion" {
        val mockClientEntity = mockk<LedStripClientEntity>()
        setupCommonMocks(mockClientEntity, ClientType.Pi)
        val expectedJobState = PiStreamingJobState(StreamingJobStatus.SetupIncomplete)
        val callbackSlot = slot<CompletionHandler>()

        every { mockJob.invokeOnCompletion(capture(callbackSlot)) } returns mockDisposable

        val manager = StreamJobManagerImpl(mockStreamingJobFactory)
        manager.startStreamingJob(mockClientEntity)

        verify { mockStreamingJobFactory.createJob(mockClientEntity) }
        verify { mockStreamingJob.start(any()) }

        var jobState = manager.getJobState(mockClientEntity.uuid!!)
        jobState shouldBe expectedJobState

        // Job callback indicating it completed
        // This can happen if the client is deleted from the database
        val callback = callbackSlot.captured
        callback.invoke(null)

        jobState = manager.getJobState(mockClientEntity.uuid!!)
        jobState shouldBe null
    }
})