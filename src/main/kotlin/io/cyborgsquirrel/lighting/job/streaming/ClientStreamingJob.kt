package io.cyborgsquirrel.lighting.job.streaming

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job

interface ClientStreamingJob: DisposableHandle {

    fun start(scope: CoroutineScope): Job

    fun onDataUpdate()
}