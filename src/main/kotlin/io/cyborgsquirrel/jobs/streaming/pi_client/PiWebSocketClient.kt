package io.cyborgsquirrel.jobs.streaming.pi_client

import io.micronaut.websocket.CloseReason
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.*
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue

@ClientWebSocket
abstract class PiWebSocketClient : AutoCloseable {

    private var session: WebSocketSession? = null
    private var onDisconnectedCallback: () -> Unit = {}
    val responseQueue = ConcurrentLinkedQueue<ByteArray>()

    fun registerOnDisconnectedCallback(callback: () -> Unit) {
        onDisconnectedCallback = callback
    }

    fun unregisterOnDisconnectedCallback() {
        onDisconnectedCallback = {}
    }

    @OnMessage
    fun onMessage(message: ByteArray) {
        logger.debug("Message - {}", message)
        responseQueue.add(message)
    }

    @OnOpen
    fun onOpen(session: WebSocketSession) {
        this.session = session
    }

    @OnClose
    fun onClose(closeReason: CloseReason) {
        logger.info("WebSocket closed - cause: $closeReason")
        session = null
        notifyDisconnectCallback()
    }

    @OnError
    fun onError(error: Throwable) {
        logger.error("WebSocket error! ${error.javaClass} ${error.message}")
        session = null
        notifyDisconnectCallback()
    }

    fun send(message: ByteArray): CompletableFuture<ByteArray> = session!!.sendAsync(message)

    private fun notifyDisconnectCallback() {
        try {
            onDisconnectedCallback()
        } catch (_: Exception) {
            // Suppress exceptions
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PiWebSocketClient::class.java)
    }
}
