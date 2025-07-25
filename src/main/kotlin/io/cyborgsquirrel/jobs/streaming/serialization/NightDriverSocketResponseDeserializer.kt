package io.cyborgsquirrel.jobs.streaming.serialization

import io.cyborgsquirrel.jobs.streaming.nightdriver.NightDriverSocketResponse
import io.micronaut.core.serialize.exceptions.SerializationException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NightDriverSocketResponseDeserializer {

    fun deserialize(bytes: ByteArray): NightDriverSocketResponse {
        if (bytes.size != NightDriverSocketResponse.SIZE_IN_BYTES) {
            throw SerializationException("Unexpected size for NightDriverSocketResponse ${bytes.size} should be ${NightDriverSocketResponse.SIZE_IN_BYTES}")
        }

        var offset = 0
        var buffer = ByteBuffer.wrap(bytes.sliceArray(offset..offset + 3))
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN)
        val size = buffer.getShort()
        buffer.clear()

        if (size.toInt() != NightDriverSocketResponse.SIZE_IN_BYTES) {
            throw SerializationException("Unexpected size for NightDriverSocketResponse $size should be ${NightDriverSocketResponse.SIZE_IN_BYTES}")
        }

        offset += 4
        buffer = ByteBuffer.wrap(bytes.sliceArray(offset..offset + 3))
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN)
        val sequence = buffer.getInt()
        buffer.clear()

        offset += 8
        buffer = ByteBuffer.wrap(bytes.sliceArray(offset..offset + 3))
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN)
        val flashVersion = buffer.getShort()
        buffer.clear()

        offset += 4
        buffer = ByteBuffer.wrap(bytes.sliceArray(offset..offset + 7))
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN)
        val currentClock = buffer.getDouble()

        offset += 8
        buffer = ByteBuffer.wrap(bytes.sliceArray(offset..offset + 7))
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN)
        val oldestPacket = buffer.getDouble()

        offset += 8
        buffer = ByteBuffer.wrap(bytes.sliceArray(offset..offset + 7))
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN)
        val newestPacket = buffer.getDouble()

        offset += 8
        buffer = ByteBuffer.wrap(bytes.sliceArray(offset..offset + 7))
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN)
        val brightness = buffer.getDouble()

        offset += 8
        buffer = ByteBuffer.wrap(bytes.sliceArray(offset..offset + 7))
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN)
        val wifiSignal = buffer.getDouble()

        offset += 8
        buffer = ByteBuffer.wrap(bytes.sliceArray(offset..offset + 3))
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN)
        val bufferSize = buffer.getShort()

        offset += 4
        buffer = ByteBuffer.wrap(bytes.sliceArray(offset..offset + 3))
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN)
        val bufferPosition = buffer.getShort()

        offset += 4
        buffer = ByteBuffer.wrap(bytes.sliceArray(offset..offset + 3))
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN)
        val frameDrawing = buffer.getShort()

        offset += 4
        buffer = ByteBuffer.wrap(bytes.sliceArray(offset..offset + 3))
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN)
        val watts = buffer.getShort()

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
            watts
        )
    }
}
