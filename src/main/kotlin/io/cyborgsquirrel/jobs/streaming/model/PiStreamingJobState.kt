package io.cyborgsquirrel.jobs.streaming.model

class PiStreamingJobState(
    override val status: StreamingJobStatus
) : StreamingJobState(status)