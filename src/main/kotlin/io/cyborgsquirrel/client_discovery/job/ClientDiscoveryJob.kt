package io.cyborgsquirrel.client_discovery.job

import io.cyborgsquirrel.client_discovery.enums.DiscoveryJobStatus
import io.cyborgsquirrel.client_discovery.model.ClientDiscoveryResponse
import io.cyborgsquirrel.client_discovery.udp.UdpBroadcastReceiver
import io.cyborgsquirrel.client_discovery.udp.UdpBroadcastSender
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

    fun getDiscoveryResponses(): Set<ClientDiscoveryResponse> {
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