package io.cyborgsquirrel.jobs.streaming.nightdriver

import io.cyborgsquirrel.util.time.TimeHelper
import io.micronaut.core.serialize.exceptions.SerializationException
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class NightDriverSocketResponse(
    val size: Short, // 4 bytes
    val sequence: Int, // 8 bytes
    val flashVersion: Short, // 4 bytes
    val currentClock: Double, // 8 bytes - seconds since epoch
    val oldestPacket: Double, // 8 bytes - seconds relative to currentClock
    val newestPacket: Double, // 8 bytes - seconds relative to currentClock
    val brightness: Double, // 8 bytes
    val wifiSignal: Double, // 8 bytes
    val bufferSize: Short, // 4 bytes
    val bufferPosition: Short, // 4 bytes
    val frameDrawing: Short, // 4 bytes
    val watts: Short, // 4 bytes
    val receivedAt: Long, // Time when this was received by the server
) {

    fun currentClockMillis() = (currentClock * 1000.0).toLong()

    fun oldestPacketMillis() = (oldestPacket * 1000.0).toLong()

    fun newestPacketMillis() = (newestPacket * 1000.0).toLong()

    companion object {
        const val SIZE_IN_BYTES = 72
    }
}

fun ByteArray.toNightDriverSocketResponse(timeHelper: TimeHelper): NightDriverSocketResponse {
    if (size != NightDriverSocketResponse.SIZE_IN_BYTES) {
        throw SerializationException("Unexpected size for NightDriverSocketResponse $size should be ${NightDriverSocketResponse.SIZE_IN_BYTES}")
    }

    val buf = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)

    val size = buf.short
    buf.short // padding

    if (size.toInt() != NightDriverSocketResponse.SIZE_IN_BYTES) {
        throw SerializationException("Unexpected size in NightDriverSocketResponse header $size should be ${NightDriverSocketResponse.SIZE_IN_BYTES}")
    }

    val sequence = buf.int
    buf.int // padding

    val flashVersion = buf.short
    buf.short // padding

    val currentClock = buf.double
    val oldestPacket = buf.double
    val newestPacket = buf.double
    val brightness = buf.double
    val wifiSignal = buf.double

    val bufferSize = buf.short
    buf.short // padding

    val bufferPosition = buf.short
    buf.short // padding

    val frameDrawing = buf.short
    buf.short // padding

    val watts = buf.short

    return NightDriverSocketResponse(
        size,
        sequence,
        flashVersion,
        currentClock,
        oldestPacket,
        newestPacket,
        brightness,
        wifiSignal,
        bufferSize,
        bufferPosition,
        frameDrawing,
        watts,
        receivedAt = timeHelper.millisSinceEpoch(),
    )
}
