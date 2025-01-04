package io.cyborgsquirrel.serialization

import io.cyborgsquirrel.model.color.RgbFrameData

class RgbFrameDataSerializer {

    /**
     * Encodes [RgbFrameData] into a [ByteArray]
     */
    fun encode(frameData: RgbFrameData): ByteArray {
        val bitsPerByte = 8
        val reservedByteLen = 1
        val timestampByteLen = 8
        val rgbByteLen = 3
        val rgbDataByteLen = frameData.rgbData.size * 3
        val encodedFrame = ByteArray(reservedByteLen + timestampByteLen + rgbDataByteLen)

        encodedFrame[0] = 0

        val bitsToShift = timestampByteLen * bitsPerByte - bitsPerByte
        // Add timestamp bytes in big endian order
        for (i in 0..<timestampByteLen) {
            encodedFrame[i + reservedByteLen] =
                ((frameData.timestamp shr (bitsToShift - i * bitsPerByte)) and 0xFF).toByte()
        }

        val r = 0
        val g = 1
        val b = 2
        for (i in 0..<frameData.rgbData.size) {
            val offset = reservedByteLen + timestampByteLen + (i * rgbByteLen)
            encodedFrame[offset + r] = frameData.rgbData[i].red.toByte()
            encodedFrame[offset + g] = frameData.rgbData[i].green.toByte()
            encodedFrame[offset + b] = frameData.rgbData[i].blue.toByte()
        }

        return encodedFrame
    }
}