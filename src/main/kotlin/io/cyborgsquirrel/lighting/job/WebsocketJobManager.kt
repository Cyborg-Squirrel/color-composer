package io.cyborgsquirrel.lighting.job

import io.cyborgsquirrel.clients.entity.LedStripClientEntity

interface WebsocketJobManager {
    fun startWebsocketJob(client: LedStripClientEntity)

    fun updateJob(client: LedStripClientEntity)

    fun stopWebsocketJob(client: LedStripClientEntity)

    fun stopAllJobs()
}