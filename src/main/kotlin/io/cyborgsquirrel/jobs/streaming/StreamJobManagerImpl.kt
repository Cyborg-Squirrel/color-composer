package io.cyborgsquirrel.jobs.streaming

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.jobs.streaming.nightdriver.NightDriverSocketJob
import io.cyborgsquirrel.jobs.streaming.nightdriver.NightDriverSocketResponse
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.Semaphore

@Singleton
class StreamJobManagerImpl(
    private val streamingJobFactory: StreamingJobFactory
) : StreamJobManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobMap = mutableMapOf<String, Pair<ClientStreamingJob, Job>>()
    private val lock = Semaphore(1)

    override fun startStreamingJob(client: LedStripClientEntity) {
        logger.info("Starting websocket job for $client")
        try {
            lock.acquire()
            if (jobMap.containsKey((client.uuid!!))) {
                stopWebsocketJob(client)
            }

            val streamingJob = streamingJobFactory.createJob(client)
            val coroutineJob = streamingJob.start(scope)
            coroutineJob.invokeOnCompletion {
                jobMap.remove(client.uuid)
            }
            jobMap[client.uuid!!] = Pair(streamingJob, coroutineJob)
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            lock.release()
        }
    }

    override fun stopWebsocketJob(client: LedStripClientEntity) {
        logger.info("Stopping websocket job for $client")
        try {
            lock.acquire()
            val jobPair = jobMap.remove(client.uuid!!)
            jobPair?.first?.dispose()
            runBlocking { jobPair?.second?.cancel() }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            lock.release()
        }
    }

    override fun stopAllJobs() {
        logger.info("Stopping all websocket jobs...")
        try {
            lock.acquire()
            for (clientUuid in jobMap.keys) {
                jobMap[clientUuid]?.first?.dispose()
                runBlocking { jobMap[clientUuid]?.second?.cancel() }
            }

            jobMap.clear()
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            lock.release()
        }
    }

    override fun getJobState(clientUuid: String) = jobMap[clientUuid]?.first?.getCurrentState()

    override fun getLatestNightDriverResponse(client: LedStripClientEntity): NightDriverSocketResponse? {
        val job = jobMap[client.uuid]?.first
        return if (job is NightDriverSocketJob) {
            return job.getLatestResponse()
        } else {
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StreamJobManagerImpl::class.java)
    }
}