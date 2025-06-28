package io.cyborgsquirrel.lighting.client

import io.micronaut.websocket.CloseReason
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.*
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@ClientWebSocket
abstract class LedStripWebSocketClient : AutoCloseable {

    private var session: WebSocketSession? = null

    private var future: CompletableFuture<Unit>? = null

    private var onDisconnectedCallback: () -> Unit = {}

    fun registerOnDisconnectedCallback(callback: () -> Unit) {
        onDisconnectedCallback = callback
    }

    fun unregisterOnDisconnectedCallback() {
        onDisconnectedCallback = {}
    }

    @OnMessage
    fun onMessage(message: ByteArray) {
        future?.complete(Unit)
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
        waitForSend()
        future = CompletableFuture()
        return session!!.sendAsync(message)
    }

    private fun waitForSend() {
        future?.get(5, TimeUnit.SECONDS)
    }

    private fun notifyDisconnectCallback() {
        try {
            onDisconnectedCallback()
        } catch (_: Exception) {
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LedStripWebSocketClient::class.java)
    }
}
