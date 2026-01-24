package io.cyborgsquirrel.lighting.rendering.post_processing

import io.cyborgsquirrel.lighting.model.LedStripPoolModel
import io.cyborgsquirrel.lighting.model.RgbColor
import io.cyborgsquirrel.lighting.model.SingleLedStripModel
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class FrameSegmentationHelperTest : StringSpec({

    val helper = FrameSegmentationHelper()
    val clientUuid = "aaaa-bbbb-cccc-dddd-eeee-ffff"
    val notThisClientsUuid = "bbb1-aaa2-cccc-dddd-eeee-ffff"

    fun generateFrame(pool: LedStripPoolModel): RenderedFrameModel {
        return RenderedFrameModel(
            strip = pool,
            frameData = listOf(
                RgbColor(255u, 0u, 0u),
                RgbColor(0u, 255u, 0u),
                RgbColor(0u, 0u, 255u),
                RgbColor(255u, 255u, 0u),
                RgbColor(0u, 255u, 255u),
                RgbColor(255u, 0u, 255u),
                RgbColor(128u, 128u, 128u),
                RgbColor(64u, 64u, 64u),
                RgbColor(10u, 20u, 30u),
                RgbColor(40u, 50u, 60u)
            ),
            sequenceNumber = 0
        )
    }

    "single strip" {
        val clientStrip = mockk<SingleLedStripModel>()
        every { clientStrip.uuid } returns "strip-1"
        every { clientStrip.length } returns 10
        every { clientStrip.clientUuid } returns clientUuid

        val frame = RenderedFrameModel(
            strip = clientStrip,
            frameData = listOf(
                RgbColor(255u, 0u, 0u),
                RgbColor(0u, 255u, 0u),
                RgbColor(0u, 0u, 255u)
            ),
            sequenceNumber = 1
        )

        val result = helper.segmentFrame(listOf(clientStrip), clientUuid, frame)

        result.size shouldBe 1
        result[0].frameData shouldBe frame.frameData
        result[0].strip shouldBe clientStrip
        result[0].sequenceNumber shouldBe frame.sequenceNumber
    }

    "client strips comprise the entire pool" {
        val clientStrip1 = mockk<SingleLedStripModel>()
        every { clientStrip1.uuid } returns "strip-1"
        every { clientStrip1.length } returns 5
        every { clientStrip1.clientUuid } returns clientUuid
        every { clientStrip1.inverted } returns false

        val clientStrip2 = mockk<SingleLedStripModel>()
        every { clientStrip2.uuid } returns "strip-2"
        every { clientStrip2.length } returns 3
        every { clientStrip2.clientUuid } returns clientUuid
        every { clientStrip2.inverted } returns false

        val clientStrip3 = mockk<SingleLedStripModel>()
        every { clientStrip3.uuid } returns "strip-3"
        every { clientStrip3.length } returns 2
        every { clientStrip3.clientUuid } returns clientUuid
        every { clientStrip3.inverted } returns false

        val stripPool = mockk<LedStripPoolModel>()
        every { stripPool.uuid } returns "pool-1"
        every { stripPool.strips } returns listOf(clientStrip1, clientStrip2, clientStrip3)

        val frame = generateFrame(stripPool)
        val result = helper.segmentFrame(listOf(stripPool), clientUuid, frame)

        result.size shouldBe 3
        result[0].frameData shouldBe frame.frameData.subList(0, clientStrip1.length)
        result[0].strip shouldBe clientStrip1
        result[0].sequenceNumber shouldBe frame.sequenceNumber
        result[1].frameData shouldBe frame.frameData.subList(
            clientStrip1.length,
            clientStrip1.length + clientStrip2.length
        )
        result[1].strip shouldBe clientStrip2
        result[1].sequenceNumber shouldBe frame.sequenceNumber
        result[2].frameData shouldBe frame.frameData.subList(
            clientStrip1.length + clientStrip2.length,
            frame.frameData.size
        )
        result[2].strip shouldBe clientStrip3
        result[2].sequenceNumber shouldBe frame.sequenceNumber
    }

    "client is a subset of the pool" {
        val clientStrip1 = mockk<SingleLedStripModel>()
        every { clientStrip1.uuid } returns "strip-1"
        every { clientStrip1.length } returns 5
        every { clientStrip1.clientUuid } returns clientUuid
        every { clientStrip1.inverted } returns false

        val clientStrip2 = mockk<SingleLedStripModel>()
        every { clientStrip2.uuid } returns "strip-2"
        every { clientStrip2.length } returns 3
        every { clientStrip2.clientUuid } returns clientUuid
        every { clientStrip2.inverted } returns false

        val poolStrip3 = mockk<SingleLedStripModel>()
        every { poolStrip3.uuid } returns "strip-3"
        every { poolStrip3.length } returns 2
        every { poolStrip3.clientUuid } returns "not-this-clients-uuid"
        every { poolStrip3.inverted } returns false

        val poolStrip = mockk<LedStripPoolModel>()
        every { poolStrip.uuid } returns "pool-1"
        every { poolStrip.strips } returns listOf(clientStrip1, clientStrip2, poolStrip3)

        val frame = generateFrame(poolStrip)
        val result = helper.segmentFrame(listOf(poolStrip), clientUuid, frame)

        result.size shouldBe 2
        result[0].frameData shouldBe frame.frameData.subList(0, clientStrip1.length)
        result[0].strip shouldBe clientStrip1
        result[0].sequenceNumber shouldBe frame.sequenceNumber
        result[1].frameData shouldBe frame.frameData.subList(
            clientStrip1.length,
            clientStrip1.length + clientStrip2.length
        )
        result[1].strip shouldBe clientStrip2
        result[1].sequenceNumber shouldBe frame.sequenceNumber
    }

    "client is a non-contiguous subset of the pool" {
        val clientStrip1 = mockk<SingleLedStripModel>()
        every { clientStrip1.uuid } returns "strip-1"
        every { clientStrip1.length } returns 5
        every { clientStrip1.clientUuid } returns clientUuid
        every { clientStrip1.inverted } returns false

        val clientStrip2 = mockk<SingleLedStripModel>()
        every { clientStrip2.uuid } returns "strip-2"
        every { clientStrip2.length } returns 3
        every { clientStrip2.clientUuid } returns clientUuid
        every { clientStrip2.inverted } returns false

        val poolStrip2 = mockk<SingleLedStripModel>()
        every { poolStrip2.uuid } returns "strip-3"
        every { poolStrip2.length } returns 2
        every { poolStrip2.clientUuid } returns notThisClientsUuid
        every { poolStrip2.inverted } returns false

        val poolStrip = mockk<LedStripPoolModel>()
        every { poolStrip.uuid } returns "pool-1"
        every { poolStrip.strips } returns listOf(clientStrip1, poolStrip2, clientStrip2)
        every { poolStrip2.inverted } returns false

        val frame = generateFrame(poolStrip)
        val result = helper.segmentFrame(listOf(poolStrip), clientUuid, frame)

        result.size shouldBe 2
        result[0].frameData shouldBe frame.frameData.subList(0, clientStrip1.length)
        result[0].strip shouldBe clientStrip1
        result[0].sequenceNumber shouldBe frame.sequenceNumber
        result[1].frameData shouldBe frame.frameData.subList(
            clientStrip1.length + poolStrip2.length,
            frame.frameData.size
        )
        result[1].strip shouldBe clientStrip2
        result[1].sequenceNumber shouldBe frame.sequenceNumber
    }

    "strip pool with inverted strip" {
        val clientStrip1 = mockk<SingleLedStripModel>()
        every { clientStrip1.uuid } returns "strip-1"
        every { clientStrip1.length } returns 5
        every { clientStrip1.clientUuid } returns clientUuid
        every { clientStrip1.inverted } returns false

        val clientStrip2 = mockk<SingleLedStripModel>()
        every { clientStrip2.uuid } returns "strip-2"
        every { clientStrip2.length } returns 3
        every { clientStrip2.clientUuid } returns clientUuid
        every { clientStrip2.inverted } returns true

        val poolStrip = mockk<LedStripPoolModel>()
        every { poolStrip.uuid } returns "pool-1"
        every { poolStrip.strips } returns listOf(clientStrip1, clientStrip2)

        val frame = generateFrame(poolStrip)
        val result = helper.segmentFrame(listOf(poolStrip), clientUuid, frame)

        result.size shouldBe 2
        result[0].frameData shouldBe frame.frameData.subList(0, clientStrip1.length)
        result[0].strip shouldBe clientStrip1
        result[0].sequenceNumber shouldBe frame.sequenceNumber

        val expectedInvertedData =
            frame.frameData.subList(clientStrip1.length, clientStrip1.length + clientStrip2.length).reversed()
        result[1].frameData shouldBe expectedInvertedData
        result[1].strip shouldBe clientStrip2
        result[1].sequenceNumber shouldBe frame.sequenceNumber
    }
})