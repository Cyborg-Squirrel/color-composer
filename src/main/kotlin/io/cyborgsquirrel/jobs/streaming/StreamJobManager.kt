package io.cyborgsquirrel.jobs.streaming

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.jobs.streaming.model.StreamingJobState
import io.cyborgsquirrel.jobs.streaming.model.StreamingJobStatus
import io.cyborgsquirrel.jobs.streaming.nightdriver.NightDriverSocketResponse

interface StreamJobManager {
    fun startStreamingJob(client: LedStripClientEntity)

    fun stopWebsocketJob(client: LedStripClientEntity)

    fun stopAllJobs()

    fun getJobState(client: LedStripClientEntity): StreamingJobState?

    fun getLatestNightDriverResponse(client: LedStripClientEntity): NightDriverSocketResponse?
}