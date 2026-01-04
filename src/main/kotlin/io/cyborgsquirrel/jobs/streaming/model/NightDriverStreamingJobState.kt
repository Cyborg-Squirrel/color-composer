package io.cyborgsquirrel.jobs.streaming.model

import io.cyborgsquirrel.jobs.streaming.nightdriver.NightDriverSocketResponse

data class NightDriverStreamingJobState(
    override val status: StreamingJobStatus,
    val latestResponse: NightDriverSocketResponse?
) : StreamingJobState(status)