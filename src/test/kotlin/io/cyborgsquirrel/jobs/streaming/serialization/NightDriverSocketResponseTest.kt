package io.cyborgsquirrel.jobs.streaming.serialization

import io.cyborgsquirrel.jobs.streaming.nightdriver.NightDriverSocketResponse
import io.cyborgsquirrel.jobs.streaming.nightdriver.toNightDriverSocketResponse
import io.cyborgsquirrel.util.time.TimeHelper
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

@AnnotationSpec.Test
class NightDriverSocketResponseTest : StringSpec({

    val timeHelper = mockk<TimeHelper>()

    "Deserialize socket response from Night Driver" {
        every { timeHelper.millisSinceEpoch() } returns 12345L

        val bytes = byteArrayOf(
            72,
            0,
            0,
            0,
            41,
            0,
            2,
            0,
            0,
            0,
            0,
            0,
            40,
            0,
            0,
            0,
            -71,
            -86,
            -20,
            -2,
            15,
            33,
            -38,
            65,
            0,
            0,
            0,
            0,
            -101,
            85,
            -26,
            -65,
            0,
            0,
            0,
            0,
            101,
            84,
            -45,
            63,
            0,
            0,
            0,
            96,
            90,
            90,
            54,
            64,
            0,
            0,
            0,
            0,
            0,
            -128,
            69,
            -64,
            -76,
            0,
            0,
            0,
            15,
            0,
            0,
            0,
            30,
            0,
            0,
            0,
            2,
            0,
            0,
            0
        )

        val socketResponse = bytes.toNightDriverSocketResponse(timeHelper)

        socketResponse.size shouldBe NightDriverSocketResponse.SIZE_IN_BYTES
        socketResponse.sequence shouldBe 131113
        socketResponse.flashVersion shouldBe 40
        socketResponse.currentClockMillis() shouldBe 1753497595697L
        socketResponse.oldestPacketMillis() shouldBe -697L
        socketResponse.newestPacketMillis() shouldBe 302L
        socketResponse.brightness shouldBeGreaterThan 22.3
        socketResponse.brightness shouldBeLessThan 22.4
        socketResponse.wifiSignal shouldBe -43.0
        socketResponse.bufferSize shouldBe 180
        socketResponse.bufferPosition shouldBe 15
        socketResponse.frameDrawing shouldBe 30
        socketResponse.watts shouldBe 2
        socketResponse.receivedAt shouldBe 12345L
    }

    "Wrong size packet" {
        var didThrow = false
        try {
            byteArrayOf(1, 2, 3).toNightDriverSocketResponse(timeHelper)
        } catch (ex: Exception) {
            didThrow = true
        }

        didThrow shouldBe true
    }

    "Wrong size in packet size field" {
        every { timeHelper.millisSinceEpoch() } returns 0L

        val bytes = byteArrayOf(
            73,
            0,
            0,
            0,
            41,
            0,
            2,
            0,
            0,
            0,
            0,
            0,
            40,
            0,
            0,
            0,
            -71,
            -86,
            -20,
            -2,
            15,
            33,
            -38,
            65,
            0,
            0,
            0,
            0,
            -101,
            85,
            -26,
            -65,
            0,
            0,
            0,
            0,
            101,
            84,
            -45,
            63,
            0,
            0,
            0,
            96,
            90,
            90,
            54,
            64,
            0,
            0,
            0,
            0,
            0,
            -128,
            69,
            -64,
            -76,
            0,
            0,
            0,
            15,
            0,
            0,
            0,
            30,
            0,
            0,
            0,
            2,
            0,
            0,
            0
        )
        var didThrow = false
        try {
            bytes.toNightDriverSocketResponse(timeHelper)
        } catch (ex: Exception) {
            didThrow = true
        }

        didThrow shouldBe true
    }
})
