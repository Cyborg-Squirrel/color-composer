package io.cyborgsquirrel.jobs.streaming.serialization

import io.cyborgsquirrel.lighting.model.RgbFrameData
import io.cyborgsquirrel.util.toLittleEndian

class NightDriverFrameDataSerializer {

    private val commandBytesLen = 2
    private val lengthBytesLen = 4
    private val timestampBytesLen = 8
    private val channelBytesLen = 2
    private val rgbLen = 3

    /**
     * Encodes [RgbFrameData] into a [ByteArray]
     * [channelBitmask] is a bitmask of channels to display the RgbFrameData on
     */
    fun encode(frameData: RgbFrameData, channelBitmask: Int): ByteArray {
        val rgbDataBytesLen = frameData.rgbData.size * 3
        val totalFrameBytes =
            commandBytesLen + channelBytesLen + lengthBytesLen + (timestampBytesLen * 2) + rgbDataBytesLen
        val encodedFrame = ByteArray(totalFrameBytes)

        // NightDriver command is 0x03 for streaming RGB data
        val commandBytes = 0x03.toLittleEndian(2)
        for (i in 0..<commandBytesLen) {
            encodedFrame[i] = commandBytes[i]
        }

        var offset = commandBytesLen
        val channelBitmaskBytes = channelBitmask.toLittleEndian(channelBytesLen)
        for (i in 0 until channelBytesLen) {
            encodedFrame[i + offset] = channelBitmaskBytes[i]
        }

        offset = commandBytesLen + channelBytesLen

        val frameDataLenBytes = frameData.rgbData.size.toLittleEndian(lengthBytesLen)
        for (i in 0 until lengthBytesLen) {
            encodedFrame[i + offset] = frameDataLenBytes[i]
        }

        offset = commandBytesLen + channelBytesLen + lengthBytesLen

        // Add timestamp bytes in little endian order
        // Total whole seconds
        val seconds = frameData.timestamp / 1000
        val secondsBytes = seconds.toLittleEndian(timestampBytesLen)
        for (i in 0 until timestampBytesLen) {
            encodedFrame[i + offset] = secondsBytes[i]
        }

        offset = commandBytesLen + channelBytesLen + lengthBytesLen + timestampBytesLen

        // Time (in microseconds) remaining from timestamp after subtracting whole seconds
        val micros = (frameData.timestamp % 1000) * 1000
        val microsBytes = micros.toLittleEndian(timestampBytesLen)
        for (i in 0 until timestampBytesLen) {
            encodedFrame[i + offset] = microsBytes[i]
        }

        offset = commandBytesLen + channelBytesLen + lengthBytesLen + (timestampBytesLen * 2)

        for (i in 0..<frameData.rgbData.size) {
            val rgbOffset = offset + (i * rgbLen)
            encodedFrame[rgbOffset + 0] = frameData.rgbData[i].red.toByte()
            encodedFrame[rgbOffset + 1] = frameData.rgbData[i].green.toByte()
            encodedFrame[rgbOffset + 2] = frameData.rgbData[i].blue.toByte()
        }

        return encodedFrame
    }
}