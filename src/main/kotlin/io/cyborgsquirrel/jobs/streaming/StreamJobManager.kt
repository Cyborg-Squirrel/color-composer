package io.cyborgsquirrel.jobs.streaming

import io.cyborgsquirrel.clients.entity.LedStripClientEntity
import io.cyborgsquirrel.jobs.streaming.nightdriver.NightDriverSocketResponse

interface StreamJobManager {
    fun startWebsocketJob(client: LedStripClientEntity)

    fun notifyJobOfDataUpdate(client: LedStripClientEntity)

    fun stopWebsocketJob(client: LedStripClientEntity)

    fun stopAllJobs()

    fun getJobState(client: LedStripClientEntity): StreamingJobState?

    fun getLatestNightDriverResponse(client: LedStripClientEntity): NightDriverSocketResponse?
}