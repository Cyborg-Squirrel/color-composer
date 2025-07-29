package io.cyborgsquirrel.jobs.streaming.serialization

import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.RgbFrameData
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@AnnotationSpec.Test
class NightDriverFrameDataSerializerTest : StringSpec({

    val timestamp = 1727921898452
    val rgbData =
        listOf(
            RgbColor(0u, 0u, 255u),
            RgbColor(0u, 255u, 0u),
            RgbColor(255u, 0u, 0u),
            RgbColor(128u, 127u, 200u),
        )

    "Serialize a frame" {
        val serializer = NightDriverFrameDataSerializer()
        val rgbFrameData = RgbFrameData(timestamp, rgbData)

        // NightDriver uses channels instead of pins like the Raspberry Pi
        // this value is a bitmask of which channels (pins) the NightDriver client should display the RGB values on
        val channelBitmask = 5

        val encodedBytes = serializer.encode(rgbFrameData, channelBitmask)

        // Encoded bytes include:
        // Command (2)
        // Channel (2)
        // RGB data length (4)
        // Seconds (8)
        // Microseconds (8)
        // Four RgbPixels which are three bytes each (12)
        encodedBytes.size shouldBe 36

        // Command byte
        encodedBytes[0] shouldBe 3
        encodedBytes[1] shouldBe 0x00

        // Channel bytes
        encodedBytes[2] shouldBe 0x05
        encodedBytes[3] shouldBe 0x00

        // RGB data length
        encodedBytes[4] shouldBe 0x04
        encodedBytes[5] shouldBe 0x00
        encodedBytes[6] shouldBe 0x00
        encodedBytes[7] shouldBe 0x00

        // Seconds bytes
        encodedBytes[8] shouldBe -0x16
        encodedBytes[9] shouldBe -0x02
        encodedBytes[10] shouldBe -0x03
        encodedBytes[11] shouldBe 0x66
        encodedBytes[12] shouldBe 0x00
        encodedBytes[13] shouldBe 0x00
        encodedBytes[14] shouldBe 0x00
        encodedBytes[15] shouldBe 0x00

        // Microseconds bytes
        encodedBytes[16] shouldBe -0x60
        encodedBytes[17] shouldBe -0x1B
        encodedBytes[18] shouldBe 0x06
        encodedBytes[19] shouldBe 0x00
        encodedBytes[20] shouldBe 0x00
        encodedBytes[21] shouldBe 0x00
        encodedBytes[22] shouldBe 0x00
        encodedBytes[23] shouldBe 0x00

        var byteDataIndex = 24
        // RgbPixel bytes
        for (data in rgbData) {
            encodedBytes[byteDataIndex] shouldBe data.red.toByte()
            encodedBytes[byteDataIndex + 1] shouldBe data.green.toByte()
            encodedBytes[byteDataIndex + 2] shouldBe data.blue.toByte()
            byteDataIndex += 3
        }
    }
})
