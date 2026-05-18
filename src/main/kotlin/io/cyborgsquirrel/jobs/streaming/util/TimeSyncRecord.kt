package io.cyborgsquirrel.jobs.streaming.util

data class TimeSyncRecord(val performedAt: Long, val clientTimeOffset: Long, val networkLatency: Long)