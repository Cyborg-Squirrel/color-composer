package io.cyborgsquirrel.jobs.streaming.model

sealed class StreamingJobState(open val status: StreamingJobStatus)