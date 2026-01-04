package io.cyborgsquirrel.jobs.streaming.model

import io.cyborgsquirrel.jobs.streaming.nightdriver.NightDriverSocketResponse

class NightDriverStreamingJobState(
    override val status: StreamingJobStatus,
    val latestResponse: NightDriverSocketResponse?
) : StreamingJobState(status)