package io.cyborgsquirrel.lighting.client

import io.micronaut.websocket.CloseReason
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.*
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit

@ClientWebSocket
abstract class LedStripWebSocketClient : AutoCloseable {
    private val messageHistory: Deque<ByteArray> = ConcurrentLinkedDeque()

    val latestMessage: ByteArray
        get() = messageHistory.peekLast()

    val messagesChronologically: List<ByteArray>
        get() = ArrayList(messageHistory)

    private var session: WebSocketSession? = null

    private var future: CompletableFuture<Unit>? = null

    @OnMessage
    fun onMessage(message: ByteArray) {
        future?.complete(Unit)
        messageHistory.add(message)
        if (messageHistory.size > 5) {
            messageHistory.removeFirst()
        }
    }

    @OnOpen
    fun onOpen(session: WebSocketSession) {
        this.session = session
    }

    @OnClose
    fun onClose(closeReason: CloseReason) {
        session = null
    }

    @OnError
    fun onError(error: Throwable) {

    }

    fun send(message: ByteArray): CompletableFuture<ByteArray> {
        waitForSend()
        future = CompletableFuture()
        return session!!.sendAsync(message)
    }

    private fun waitForSend() {
        future?.get(5, TimeUnit.SECONDS)
    }
}
