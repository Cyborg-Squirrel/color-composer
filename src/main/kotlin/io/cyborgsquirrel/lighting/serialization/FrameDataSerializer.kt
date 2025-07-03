package io.cyborgsquirrel.lighting.serialization

import io.cyborgsquirrel.lighting.model.RgbFrameData
import io.cyborgsquirrel.lighting.model.RgbFrameOptions

class FrameDataSerializer {

    /**
     * Encodes [RgbFrameData] into a [ByteArray] with no [RgbFrameOptions] set
     * [pin] is the data pin the LED strip is connected to which will render this frame
     */
    fun encode(frameData: RgbFrameData, pin: String): ByteArray = encode(frameData, pin, RgbFrameOptions.blank())

    /**
     * Encodes [RgbFrameData] into a [ByteArray] with the specified [RgbFrameOptions]
     * [pin] is the data pin the LED strip is connected to which will render this frame
     */
    fun encode(frameData: RgbFrameData, pin: String, options: RgbFrameOptions): ByteArray {
        val bitsPerByte = 8
        val reservedByteLen = 1
        val timestampByteLen = 8
        val pinByteLen = 4
        val rgbByteLen = 3
        val rgbDataByteLen = frameData.rgbData.size * 3
        val encodedFrame = ByteArray(reservedByteLen + pinByteLen + timestampByteLen + rgbDataByteLen)

        encodedFrame[0] = options.byte

        val pinAsciiList = pin.toByteArray(Charsets.US_ASCII)
        if (pinAsciiList.isEmpty() || pinAsciiList.size > 4) {
            throw Exception("Invalid pin $pin")
        }

        var offset = reservedByteLen
        val paddingSize = pinByteLen - pinAsciiList.size
        for (i in 0..<paddingSize) {
            // Add white space until beginning of the pin string this section of the frame is always 4 bytes
            encodedFrame[i + offset] = 0x20
        }

        offset = reservedByteLen + paddingSize
        for (i in pinAsciiList.indices) {
            encodedFrame[i + offset] = pinAsciiList[i]
        }

        // Add timestamp bytes in little endian order
        offset = reservedByteLen + pinByteLen
        for (i in 0 until timestampByteLen) {
            encodedFrame[i + offset] =
                ((frameData.timestamp shr (i * bitsPerByte)) and 0xFF).toByte()
        }

        val r = 0
        val g = 1
        val b = 2
        for (i in 0..<frameData.rgbData.size) {
            offset = reservedByteLen + pinByteLen + timestampByteLen + (i * rgbByteLen)
            encodedFrame[offset + r] = frameData.rgbData[i].red.toByte()
            encodedFrame[offset + g] = frameData.rgbData[i].green.toByte()
            encodedFrame[offset + b] = frameData.rgbData[i].blue.toByte()
        }

        return encodedFrame
    }
}