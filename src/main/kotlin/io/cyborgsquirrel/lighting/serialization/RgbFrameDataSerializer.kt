package io.cyborgsquirrel.lighting.serialization

import io.cyborgsquirrel.lighting.model.RgbFrameData
import io.cyborgsquirrel.lighting.model.RgbFrameOptions

class RgbFrameDataSerializer {

    /**
     * Encodes [RgbFrameData] into a [ByteArray] with no [RgbFrameOptions] set
     */
    fun encode(frameData: RgbFrameData): ByteArray = encode(frameData, RgbFrameOptions.blank())

    /**
     * Encodes [RgbFrameData] into a [ByteArray] with the specified [RgbFrameOptions]
     */
    fun encode(frameData: RgbFrameData, options: RgbFrameOptions): ByteArray {
        val bitsPerByte = 8
        val reservedByteLen = 1
        val timestampByteLen = 8
        val rgbByteLen = 3
        val rgbDataByteLen = frameData.rgbData.size * 3
        val encodedFrame = ByteArray(reservedByteLen + timestampByteLen + rgbDataByteLen)

        encodedFrame[0] = options.byte

        // Add timestamp bytes in little endian order
        for (i in 0 until timestampByteLen) {
            encodedFrame[i + reservedByteLen] =
                ((frameData.timestamp shr (i * bitsPerByte)) and 0xFF).toByte()
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