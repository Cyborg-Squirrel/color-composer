package io.cyborgsquirrel.net

import io.cyborgsquirrel.model.client.DiscoveryResponse
import io.micronaut.serde.ObjectMapper
import org.slf4j.LoggerFactory
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException

class UdpBroadcastReceiver(private val objectMapper: ObjectMapper) {

    private val discoveryResponses = mutableSetOf<DiscoveryResponse>()

    private var cancelled = false

    fun getDiscoveryResponses(): Set<DiscoveryResponse> {
        return discoveryResponses
    }

    fun cancel() {
        cancelled = true
    }

    fun receiveResponses(port: Int) {
        cancelled = false
        try {
            discoveryResponses.clear()
            DatagramSocket(port).use { socket ->
                socket.soTimeout = 5000

                val buffer = ByteArray(BUFFER_SIZE)
                val packet = DatagramPacket(buffer, buffer.size)

                logger.info("Waiting for responses...")
                while (!cancelled) {
                    try {
                        socket.receive(packet)

                        // Don't add new responses if the client with the same address already responded
                        if (!discoveryResponses.map { it.address }.contains(packet.address.hostAddress)) {
                            val receivedMessage = String(packet.data, 0, packet.length)
                            val senderAddress = packet.address
                            val senderPort = packet.port

                            logger.info("Received response from $senderAddress:$senderPort - $receivedMessage")

                            val response = objectMapper.readValue(receivedMessage, DiscoveryResponse::class.java)
                            response.address = packet.address.hostAddress
                            discoveryResponses.add(response)
                        }
                    } catch (e: SocketTimeoutException) {
                        logger.info("No more responses received. Exiting...")
                        break // Exit the loop if no more responses
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e.toString())
            e.printStackTrace()
        }
    }

    companion object {
        private const val BUFFER_SIZE = 1024
        private val logger = LoggerFactory.getLogger(UdpBroadcastReceiver::class.java)
    }
}