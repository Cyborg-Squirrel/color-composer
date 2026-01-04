package io.cyborgsquirrel.jobs.streaming

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.jobs.streaming.model.PiStreamingJobState
import io.cyborgsquirrel.jobs.streaming.model.StreamingJobStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job

class StreamJobManagerImplTest : StringSpec({
    "start a job for a new client" {
        val mockClientEntity = mockk<LedStripClientEntity>()
        val mockStreamingJob = mockk<ClientStreamingJob>()
        val mockStreamingJobFactory = mockk<StreamingJobFactory>()
        val mockJob = mockk<Job>()
        val mockDisposable = mockk<DisposableHandle>()
        
        every { mockClientEntity.uuid } returns "test-uuid"
        every { mockClientEntity.clientType } returns ClientType.Pi
        every { mockStreamingJobFactory.createJob(any()) } returns mockStreamingJob
        every { mockJob.invokeOnCompletion(any()) } returns mockDisposable
        every { mockStreamingJob.start(any()) } returns mockJob
        every { mockStreamingJob.getCurrentState() } returns PiStreamingJobState(StreamingJobStatus.SetupIncomplete)
        every { mockStreamingJob.dispose() } answers { }
        
        val manager = StreamJobManagerImpl(mockStreamingJobFactory)
        
        manager.startStreamingJob(mockClientEntity)
        
        val jobState = manager.getJobState(mockClientEntity)
        jobState shouldNotBe null
        verify { mockStreamingJobFactory.createJob(mockClientEntity) }
        verify { mockStreamingJob.start(any()) }
    }

    "stop a websocket job" {
        val mockClientEntity = mockk<LedStripClientEntity>()
        val mockStreamingJob = mockk<ClientStreamingJob>()
        val mockStreamingJobFactory = mockk<StreamingJobFactory>()
        val mockJob = mockk<Job>()
        val mockDisposable = mockk<DisposableHandle>()
        
        every { mockClientEntity.uuid } returns "test-uuid"
        every { mockClientEntity.clientType } returns ClientType.Pi
        every { mockStreamingJobFactory.createJob(any()) } returns mockStreamingJob
        every { mockJob.invokeOnCompletion(any()) } returns mockDisposable
        every { mockStreamingJob.start(any()) } returns mockJob
        every { mockStreamingJob.getCurrentState() } returns PiStreamingJobState(StreamingJobStatus.SetupIncomplete)
        every { mockStreamingJob.dispose() } answers { }
        coEvery { mockJob.cancel() } answers { }
        
        val manager = StreamJobManagerImpl(mockStreamingJobFactory)
        
        manager.startStreamingJob(mockClientEntity)
        manager.stopWebsocketJob(mockClientEntity)
        
        val jobState = manager.getJobState(mockClientEntity)
        jobState shouldBe null
        verify { mockStreamingJob.dispose() }
    }

    "handle multiple clients gracefully" {
        val mockClientEntityA = mockk<LedStripClientEntity>()
        val mockClientEntityB = mockk<LedStripClientEntity>()
        val mockStreamingJobA = mockk<ClientStreamingJob>()
        val mockStreamingJobB = mockk<ClientStreamingJob>()
        val mockStreamingJobFactory = mockk<StreamingJobFactory>()
        val mockJobA = mockk<Job>()
        val mockJobB = mockk<Job>()
        val mockDisposableA = mockk<DisposableHandle>()
        val mockDisposableB = mockk<DisposableHandle>()
        
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
        every { mockStreamingJobA.getCurrentState() } returns PiStreamingJobState(StreamingJobStatus.SetupIncomplete)
        every { mockStreamingJobB.getCurrentState() } returns PiStreamingJobState(StreamingJobStatus.SetupIncomplete)
        every { mockStreamingJobA.dispose() } answers { }
        every { mockStreamingJobB.dispose() } answers { }
        
        val manager = StreamJobManagerImpl(mockStreamingJobFactory)
        
        manager.startStreamingJob(mockClientEntityA)
        manager.startStreamingJob(mockClientEntityB)
        
        val jobState1 = manager.getJobState(mockClientEntityA)
        val jobState2 = manager.getJobState(mockClientEntityB)
        jobState1 shouldNotBe null
        jobState2 shouldNotBe null
    }
})