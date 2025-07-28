package io.cyborgsquirrel.jobs.streaming.serialization

import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.led_strips.enums.PiClientPin
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.RgbFrameData
import io.cyborgsquirrel.lighting.model.RgbFrameOptions
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@AnnotationSpec.Test
class PiFrameDataSerializerTest : StringSpec({

    val timestamp = 1727921898452
    val rgbData =
        listOf(
            RgbColor(0u, 0u, 255u),
            RgbColor(0u, 255u, 0u),
            RgbColor(255u, 0u, 0u),
            RgbColor(128u, 127u, 200u),
        )

    "Serialize a frame" {
        val serializer = PiFrameDataSerializer()
        val rgbFrameData = RgbFrameData(timestamp, rgbData)
        val pin = PiClientPin.D10.pinName

        val encodedBytes = serializer.encode(rgbFrameData, pin)

        // Encoded bytes include:
        // Command byte (1)
        // Pin (4)
        // Timestamp (8)
        // Four RgbPixels which are three bytes each (12)
        encodedBytes.size shouldBe 25

        // Reserved byte
        encodedBytes[0] shouldBe 0

        // Pin bytes
        encodedBytes[1] shouldBe 0x20
        encodedBytes[2] shouldBe 0x44
        encodedBytes[3] shouldBe 0x31
        encodedBytes[4] shouldBe 0x30

        // Timestamp bytes
        encodedBytes[5] shouldBe -0x2C
        encodedBytes[6] shouldBe -0x3D
        encodedBytes[7] shouldBe 0x2B
        encodedBytes[8] shouldBe 0x50
        encodedBytes[9] shouldBe -0x6E
        encodedBytes[10] shouldBe 0x01
        encodedBytes[11] shouldBe 0x00
        encodedBytes[12] shouldBe 0x00

        var byteDataIndex = 13
        // RgbPixel bytes
        for (data in rgbData) {
            encodedBytes[byteDataIndex] shouldBe data.red.toByte()
            encodedBytes[byteDataIndex + 1] shouldBe data.green.toByte()
            encodedBytes[byteDataIndex + 2] shouldBe data.blue.toByte()
            byteDataIndex += 3
        }
    }

    "Serialize a frame in GRB order" {
        val serializer = PiFrameDataSerializer()
        val rgbFrameData = RgbFrameData(timestamp, rgbData)
        val pin = PiClientPin.D10.pinName

        val encodedBytes = serializer.encode(rgbFrameData, pin, RgbFrameOptions.blank(), ColorOrder.GRB)

        // Encoded bytes include:
        // Command byte (1)
        // Pin (4)
        // Timestamp (8)
        // Four RgbPixels which are three bytes each (12)
        encodedBytes.size shouldBe 25

        // Reserved byte
        encodedBytes[0] shouldBe 0

        // Pin bytes
        encodedBytes[1] shouldBe 0x20
        encodedBytes[2] shouldBe 0x44
        encodedBytes[3] shouldBe 0x31
        encodedBytes[4] shouldBe 0x30

        // Timestamp bytes
        encodedBytes[5] shouldBe -0x2C
        encodedBytes[6] shouldBe -0x3D
        encodedBytes[7] shouldBe 0x2B
        encodedBytes[8] shouldBe 0x50
        encodedBytes[9] shouldBe -0x6E
        encodedBytes[10] shouldBe 0x01
        encodedBytes[11] shouldBe 0x00
        encodedBytes[12] shouldBe 0x00

        var byteDataIndex = 13
        // RgbPixel bytes
        for (data in rgbData) {
            encodedBytes[byteDataIndex] shouldBe data.green.toByte()
            encodedBytes[byteDataIndex + 1] shouldBe data.red.toByte()
            encodedBytes[byteDataIndex + 2] shouldBe data.blue.toByte()
            byteDataIndex += 3
        }
    }
})