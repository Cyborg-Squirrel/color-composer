package io.cyborgsquirrel.jobs.streaming

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.jobs.streaming.nightdriver.NightDriverSocketJob
import io.cyborgsquirrel.jobs.streaming.nightdriver.NightDriverSocketResponse
import jakarta.annotation.PreDestroy
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

@Singleton
class StreamJobManagerImpl(
    private val streamingJobFactory: StreamingJobFactory
) : StreamJobManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobMap = ConcurrentHashMap<String, Pair<ClientStreamingJob, Job>>()

    override fun startStreamingJob(client: LedStripClientEntity) {
        logger.info("Starting websocket job for $client")
        try {
            val streamingJob = streamingJobFactory.createJob(client)
            val coroutineJob = streamingJob.start(scope)
            val pair = Pair(streamingJob, coroutineJob)
            coroutineJob.invokeOnCompletion {
                jobMap.remove(client.uuid, pair)
            }
            val old = jobMap.put(client.uuid, pair)
            old?.first?.dispose()
            old?.second?.cancel()
        } catch (ex: Exception) {
            logger.error("Error starting streaming job for $client", ex)
        }
    }

    override fun stopWebsocketJob(client: LedStripClientEntity) {
        logger.info("Stopping websocket job for $client")
        try {
            val jobPair = jobMap.remove(client.uuid)
            jobPair?.first?.dispose()
            jobPair?.second?.cancel()
        } catch (ex: Exception) {
            logger.error("Error stopping streaming job for $client", ex)
        }
    }

    override fun stopAllJobs() {
        logger.info("Stopping all websocket jobs...")
        try {
            val snapshot = jobMap.values.toList()
            jobMap.clear()
            for ((streamingJob, coroutineJob) in snapshot) {
                streamingJob.dispose()
                coroutineJob.cancel()
            }
        } catch (ex: Exception) {
            logger.error("Error stopping all streaming jobs", ex)
        }
    }

    @PreDestroy
    fun shutdown() {
        stopAllJobs()
        scope.cancel()
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
