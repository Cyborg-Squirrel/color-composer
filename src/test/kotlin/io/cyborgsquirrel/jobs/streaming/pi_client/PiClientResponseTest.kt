package io.cyborgsquirrel.jobs.streaming.pi_client

import io.cyborgsquirrel.util.time.TimeHelper
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import java.nio.ByteBuffer
import java.nio.ByteOrder

@AnnotationSpec.Test
class PiClientResponseTest : StringSpec({

    val timeHelper = mockk<TimeHelper>()
    val fixedTimestamp = 99999L

    beforeEach {
        every { timeHelper.millisSinceEpoch() } returns fixedTimestamp
    }

    fun buildMessage(type: Int, body: ByteArray): ByteArray {
        val payloadSize = 2 + body.size
        val buf = ByteBuffer.allocate(4 + payloadSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(payloadSize)
        buf.put(type.toByte())
        buf.put(body.size.toByte())
        buf.put(body)
        return buf.array()
    }

    fun queueDepthBody(depth: Int): ByteArray =
        ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(depth.toShort()).array()

    "Type 0 - BufferStatus parses queue depth" {
        buildMessage(0, queueDepthBody(5)).toPiClientResponse(timeHelper).shouldBeInstanceOf<PiClientResponse.BufferStatus> {
            it.framesInQueue shouldBe 5
            it.receivedAt shouldBe fixedTimestamp
        }
    }

    "Type 0 - BufferStatus with zero frames" {
        buildMessage(0, queueDepthBody(0)).toPiClientResponse(timeHelper).shouldBeInstanceOf<PiClientResponse.BufferStatus> {
            it.framesInQueue shouldBe 0
        }
    }

    "Type 0 - BufferStatus with large queue depth" {
        buildMessage(0, queueDepthBody(1000)).toPiClientResponse(timeHelper).shouldBeInstanceOf<PiClientResponse.BufferStatus> {
            it.framesInQueue shouldBe 1000
        }
    }

    "Type 1 - BackpressureError parses message" {
        buildMessage(1, "Backpressure".toByteArray(Charsets.UTF_8)).toPiClientResponse(timeHelper)
            .shouldBeInstanceOf<PiClientResponse.BackpressureError> {
                it.message shouldBe "Backpressure"
                it.receivedAt shouldBe fixedTimestamp
            }
    }

    "Type 2 - GenericError parses message" {
        buildMessage(2, "Something went wrong".toByteArray(Charsets.UTF_8)).toPiClientResponse(timeHelper)
            .shouldBeInstanceOf<PiClientResponse.GenericError> {
                it.message shouldBe "Something went wrong"
            }
    }

    "Type 3 - NoResponse parses message" {
        buildMessage(3, "No response from renderer".toByteArray(Charsets.UTF_8)).toPiClientResponse(timeHelper)
            .shouldBeInstanceOf<PiClientResponse.NoResponse> {
                it.message shouldBe "No response from renderer"
            }
    }

    "Unknown type is captured" {
        buildMessage(99, ByteArray(0)).toPiClientResponse(timeHelper).shouldBeInstanceOf<PiClientResponse.UnknownType> {
            it.type shouldBe 99
        }
    }

    "Error messages with UTF-8 characters parse correctly" {
        val message = "Erreur: dépassement de capacité"
        buildMessage(2, message.toByteArray(Charsets.UTF_8)).toPiClientResponse(timeHelper)
            .shouldBeInstanceOf<PiClientResponse.GenericError> {
                it.message shouldBe message
            }
    }
})
