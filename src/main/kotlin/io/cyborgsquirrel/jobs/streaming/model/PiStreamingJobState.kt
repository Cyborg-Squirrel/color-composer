package io.cyborgsquirrel.jobs.streaming.model

data class PiStreamingJobState(
    override val status: StreamingJobStatus
) : StreamingJobState(status)