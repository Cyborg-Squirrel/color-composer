package io.cyborgsquirrel.clients.discovery.job

import io.cyborgsquirrel.clients.discovery.enums.DiscoveryJobStatus
import io.cyborgsquirrel.clients.discovery.model.ClientDiscoveryResponse
import io.cyborgsquirrel.clients.discovery.udp.UdpBroadcastReceiver
import io.cyborgsquirrel.clients.discovery.udp.UdpBroadcastSender
import io.micronaut.serde.ObjectMapper
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory


@Singleton
class ClientDiscoveryJob(objectMapper: ObjectMapper) : Runnable {

    private val sender = UdpBroadcastSender()
    private val receiver = UdpBroadcastReceiver(objectMapper)
    private var status = DiscoveryJobStatus.idle

    fun getStatus() = status

    fun getDiscoveryResponses() = receiver.getDiscoveryResponses()

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
            logger.error("Error while doing client discovery ${e.stackTraceToString()}")
            status = DiscoveryJobStatus.error
            e.printStackTrace()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClientDiscoveryJob::class.java)
    }
}