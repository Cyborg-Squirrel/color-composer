package io.cyborgsquirrel.jobs.streaming

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.jobs.streaming.nightdriver.NightDriverSocketJob
import io.cyborgsquirrel.jobs.streaming.nightdriver.NightDriverSocketResponse
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

@Singleton
class StreamJobManagerImpl(
    private val streamingJobFactory: StreamingJobFactory
) : StreamJobManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobMap = mutableMapOf<String, Pair<ClientStreamingJob, Job>>()

    override fun startStreamingJob(client: LedStripClientEntity) {
        logger.info("Starting websocket job for $client")
        try {
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
        }
    }

    override fun stopWebsocketJob(client: LedStripClientEntity) {
        logger.info("Stopping websocket job for $client")
        try {
            val jobPair = jobMap.remove(client.uuid!!)
            jobPair?.first?.dispose()
            runBlocking { jobPair?.second?.cancel() }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun stopAllJobs() {
        logger.info("Stopping all websocket jobs...")
        try {
            for (clientUuid in jobMap.keys) {
                jobMap[clientUuid]?.first?.dispose()
                runBlocking { jobMap[clientUuid]?.second?.cancel() }
            }

            jobMap.clear()
        } catch (ex: Exception) {
            ex.printStackTrace()
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