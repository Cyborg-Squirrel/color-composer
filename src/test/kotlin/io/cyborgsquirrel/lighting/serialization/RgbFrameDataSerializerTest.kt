package io.cyborgsquirrel.lighting.serialization

import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.RgbFrameData
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.StringSpec

@AnnotationSpec.Test
class RgbFrameDataSerializerTest : StringSpec({

    "Serialize a frame" {
        val serializer = RgbFrameDataSerializer()
        val timestamp = 1727921898452
        val rgbData =
            listOf(
                RgbColor(0u, 0u, 255u),
                RgbColor(0u, 255u, 0u),
                RgbColor(255u, 0u, 0u),
                RgbColor(128u, 127u, 200u),
            )
        val rgbFrameData = RgbFrameData(timestamp, rgbData)

        val encodedBytes = serializer.encode(rgbFrameData)

        // Encoded bytes include:
        // Reserved byte (1)
        // Timestamp (8)
        // Four RgbPixels which are three bytes each (12)
        assert(encodedBytes.size == 21)

        // Reserved byte
        assert(encodedBytes[0] == 0.toByte())

        // Timestamp bytes
        assert(encodedBytes[1] == (-44).toByte())
        assert(encodedBytes[2] == (-61).toByte())
        assert(encodedBytes[3] == 43.toByte())
        assert(encodedBytes[4] == 80.toByte())
        assert(encodedBytes[5] == (-110).toByte())
        assert(encodedBytes[6] == 1.toByte())
        assert(encodedBytes[7] == 0.toByte())
        assert(encodedBytes[8] == 0.toByte())

        var byteDataIndex = 9
        // RgbPixel bytes
        for (data in rgbData) {
            assert(encodedBytes[byteDataIndex] == data.red.toByte())
            assert(encodedBytes[byteDataIndex + 1] == data.green.toByte())
            assert(encodedBytes[byteDataIndex + 2] == data.blue.toByte())
            byteDataIndex += 3
        }
    }
})