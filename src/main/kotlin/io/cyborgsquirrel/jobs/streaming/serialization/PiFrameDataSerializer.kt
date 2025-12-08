package io.cyborgsquirrel.jobs.streaming.serialization

import io.cyborgsquirrel.lighting.model.RgbFrameData
import io.cyborgsquirrel.lighting.model.RgbFrameOptions
import io.cyborgsquirrel.util.toLittleEndian

class PiFrameDataSerializer {

    private val commandByteLen = 1
    private val timestampBytesLen = 8
    private val pinBytesLen = 4
    private val rgbLen = 3

    /**
     * Encodes [RgbFrameData] into a [ByteArray] with no [RgbFrameOptions] set
     * [pin] is the data pin the LED strip is connected to which will render this frame
     */
    fun encode(frameData: RgbFrameData, pin: String): ByteArray = encode(frameData, pin, RgbFrameOptions.blank())

    /**
     * Encodes [RgbFrameData] into a [ByteArray] with the specified [RgbFrameOptions]
     * [pin] is the data pin the LED strip is connected to which will render this frame
     * [options] is the serialization options for the frame
     */
    fun encode(frameData: RgbFrameData, pin: String, options: RgbFrameOptions): ByteArray {
        val rgbDataBytesLen = frameData.rgbData.size * 3
        val totalFrameBytes = commandByteLen + pinBytesLen + timestampBytesLen + rgbDataBytesLen
        val encodedFrame = ByteArray(totalFrameBytes)

        encodedFrame[0] = options.byte

        var offset = commandByteLen
        val pinAsciiList = pin.toByteArray(Charsets.US_ASCII)
        if (pinAsciiList.isEmpty() || pinAsciiList.size > 4) {
            throw Exception("Invalid pin $pin")
        }

        val paddingSize = pinBytesLen - pinAsciiList.size
        for (i in 0..<paddingSize) {
            // Add blank space until beginning of the pin string this section of the frame is always 4 bytes
            encodedFrame[i + offset] = 0x20
        }

        offset = commandByteLen + paddingSize
        for (i in pinAsciiList.indices) {
            encodedFrame[i + offset] = pinAsciiList[i]
        }

        // Add timestamp bytes in little endian order
        offset = commandByteLen + pinBytesLen
        val timestampBytes = frameData.timestamp.toLittleEndian(timestampBytesLen)
        for (i in 0 until timestampBytesLen) {
            encodedFrame[i + offset] = timestampBytes[i]
        }

        offset = commandByteLen + pinBytesLen + timestampBytesLen

        for (i in 0..<frameData.rgbData.size) {
            val rgbOffset = offset + (i * rgbLen)
            encodedFrame[rgbOffset + 0] = frameData.rgbData[i].red.toByte()
            encodedFrame[rgbOffset + 1] = frameData.rgbData[i].green.toByte()
            encodedFrame[rgbOffset + 2] = frameData.rgbData[i].blue.toByte()
        }

        return encodedFrame
    }
}