package io.cyborgsquirrel.jobs.streaming.pi_client

import io.micronaut.websocket.CloseReason
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.*
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

@ClientWebSocket
abstract class PiWebSocketClient : AutoCloseable {

    private var session: WebSocketSession? = null
    private var onDisconnectedCallback: () -> Unit = {}
    private var onMessageCallback: (ByteArray) -> Unit = {}

    fun registerOnDisconnectedCallback(callback: () -> Unit) {
        onDisconnectedCallback = callback
    }

    fun unregisterOnDisconnectedCallback() {
        onDisconnectedCallback = {}
    }

    fun registerOnMessageCallback(callback: (ByteArray) -> Unit) {
        onMessageCallback = callback
    }

    fun unregisterOnMessageCallback() {
        onMessageCallback = {}
    }

    @OnMessage
    fun onMessage(message: ByteArray) {
        logger.debug("Message - {}", message)
        try {
            onMessageCallback(message)
        } catch (ex: Exception) {
            logger.error("Error in onMessage callback", ex)
        }
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
