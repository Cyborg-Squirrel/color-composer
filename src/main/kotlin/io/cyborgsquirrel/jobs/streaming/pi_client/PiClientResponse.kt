package io.cyborgsquirrel.jobs.streaming.pi_client

import io.cyborgsquirrel.util.time.TimeHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed class PiClientResponse {
    abstract val receivedAt: Long

    data class BufferStatus(val framesInQueue: Int, override val receivedAt: Long) : PiClientResponse()
    data class BackpressureError(val message: String, override val receivedAt: Long) : PiClientResponse()
    data class GenericError(val message: String, override val receivedAt: Long) : PiClientResponse()
    data class NoResponse(val message: String, override val receivedAt: Long) : PiClientResponse()
    data class UnknownType(val type: Int, override val receivedAt: Long) : PiClientResponse()
}

fun ByteArray.toPiClientResponse(timeHelper: TimeHelper): PiClientResponse {
    val receivedAt = timeHelper.millisSinceEpoch()
    val buf = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
    buf.int // payload length prefix
    val type = buf.get().toInt() and 0xFF
    val bodyLength = buf.get().toInt() and 0xFF
    return when (type) {
        0 -> PiClientResponse.BufferStatus(buf.short.toInt() and 0xFFFF, receivedAt)
        1 -> PiClientResponse.BackpressureError(readString(buf, bodyLength), receivedAt)
        2 -> PiClientResponse.GenericError(readString(buf, bodyLength), receivedAt)
        3 -> PiClientResponse.NoResponse(readString(buf, bodyLength), receivedAt)
        else -> PiClientResponse.UnknownType(type, receivedAt)
    }
}

private fun readString(buf: ByteBuffer, length: Int): String {
    val bytes = ByteArray(length)
    buf.get(bytes)
    return String(bytes, Charsets.UTF_8)
}
