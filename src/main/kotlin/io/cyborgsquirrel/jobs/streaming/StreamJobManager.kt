package io.cyborgsquirrel.jobs.streaming

import io.cyborgsquirrel.clients.entity.LedStripClientEntity

interface StreamJobManager {
    fun startWebsocketJob(client: LedStripClientEntity)

    fun updateJob(client: LedStripClientEntity)

    fun stopWebsocketJob(client: LedStripClientEntity)

    fun stopAllJobs()
}