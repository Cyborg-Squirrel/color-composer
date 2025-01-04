package io.cyborgsquirrel.client_discovery.udp

import org.slf4j.LoggerFactory
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UdpBroadcastSender {
    fun sendBroadcast(message: String): Int {
        try {
            DatagramSocket().use { socket ->
                socket.broadcast = true

                val buffer = message.toByteArray()
                val broadcastAddress =
                    InetAddress.getByName(BROADCAST_IP)
                val packet =
                    DatagramPacket(buffer, buffer.size, broadcastAddress, PORT)

                socket.send(packet)
                logger.info("Broadcast message sent: $message")
                return socket.localPort
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 0
    }

    companion object {
        private const val PORT = 8007
        private const val BROADCAST_IP = "230.0.0.0"
        private val logger = LoggerFactory.getLogger(UdpBroadcastSender::class.java)
    }
}
