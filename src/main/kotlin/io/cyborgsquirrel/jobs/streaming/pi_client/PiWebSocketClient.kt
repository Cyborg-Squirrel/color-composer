package io.cyborgsquirrel.jobs.streaming.pi_client

import io.micronaut.websocket.CloseReason
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.*
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
        logResponse(message)
        future?.complete(message)
    }

    private fun logResponse(response: ByteArray) {
        if (response.size < 5) return
        val buf = ByteBuffer.wrap(response).order(ByteOrder.LITTLE_ENDIAN)
        val length = buf.int
        when (val type = buf.get().toInt() and 0xFF) {
            0 -> logger.debug("Response: status, queue depth=${buf.long}")
            1 -> {
                val messageBytes = ByteArray(length - 1)
                buf.get(messageBytes)
                logger.warn("Response: error - ${String(messageBytes, Charsets.UTF_8)}")
            }
            else -> logger.warn("Response: unknown type $type")
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
