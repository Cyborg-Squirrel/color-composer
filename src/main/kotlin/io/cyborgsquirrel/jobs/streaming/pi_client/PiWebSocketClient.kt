package io.cyborgsquirrel.jobs.streaming.pi_client

import io.micronaut.websocket.CloseReason
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.*
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@ClientWebSocket
abstract class PiWebSocketClient : AutoCloseable {

    private var session: WebSocketSession? = null

    private var future: CompletableFuture<ByteArray>? = null

    private var onDisconnectedCallback: () -> Unit = {}

    fun registerOnDisconnectedCallback(callback: () -> Unit) {
        onDisconnectedCallback = callback
    }

    fun unregisterOnDisconnectedCallback() {
        onDisconnectedCallback = {}
    }

    @OnMessage
    fun onMessage(message: ByteArray) {
        logger.debug("Message - {}", message)
        future?.complete(message)
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

    fun send(message: ByteArray): CompletableFuture<ByteArray> {
        waitForResponse()
        future = CompletableFuture<ByteArray>()
        session!!.sendAsync(message)
        return future!!
    }

    fun waitForResponse() {
        future?.get(500, TimeUnit.MILLISECONDS)
    }

    private fun notifyDisconnectCallback() {
        try {
            future?.cancel(true)
        } catch (_: Exception) {
        }
        try {
            onDisconnectedCallback()
        } catch (_: Exception) {
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PiWebSocketClient::class.java)
    }
}
