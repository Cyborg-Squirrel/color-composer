package io.cyborgsquirrel.jobs.streaming

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job

interface ClientStreamingJob: DisposableHandle {

    fun start(scope: CoroutineScope): Job

    fun getCurrentState(): StreamingJobState

    fun onDataUpdate()
}