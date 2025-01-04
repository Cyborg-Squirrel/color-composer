package io.cyborgsquirrel.job

import io.cyborgsquirrel.job.enums.DiscoveryJobStatus
import io.cyborgsquirrel.model.client.DiscoveryResponse
import io.cyborgsquirrel.net.UdpBroadcastReceiver
import io.cyborgsquirrel.net.UdpBroadcastSender
import io.micronaut.serde.ObjectMapper
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory


@Singleton
class ClientDiscoveryJob(objectMapper: ObjectMapper) : Runnable {

    private val sender = UdpBroadcastSender()
    private val receiver = UdpBroadcastReceiver(objectMapper)
    private var status = DiscoveryJobStatus.idle

    fun getStatus(): DiscoveryJobStatus {
        return status
    }

    fun getDiscoveryResponses(): Set<DiscoveryResponse> {
        return receiver.getDiscoveryResponses()
    }

    fun markIdle() {
        status = DiscoveryJobStatus.idle
    }

    fun cancel() {
        receiver.cancel()
        markIdle()
    }

    override fun run() {
        logger.info("Start")
        status = DiscoveryJobStatus.inProgress
        doDiscovery()
        logger.info("Done")
    }

    private fun doDiscovery() {
        try {
            val port = sender.sendBroadcast("TEST")
            receiver.receiveResponses(port)
            logger.info("Responses: ${getDiscoveryResponses().size}")
            status = DiscoveryJobStatus.complete
//            configList.add(WebSocketJobConfig(receiver.getDiscoveredClientIPs().first(), 8765, listOf("test2")))
        } catch (e: Exception) {
            status = DiscoveryJobStatus.error
            logger.error(e.toString())
            e.printStackTrace()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClientDiscoveryJob::class.java)
    }
}