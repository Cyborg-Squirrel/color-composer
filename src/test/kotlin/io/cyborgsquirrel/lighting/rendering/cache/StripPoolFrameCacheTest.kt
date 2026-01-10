package io.cyborgsquirrel.lighting.rendering.cache

import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class StripPoolFrameCacheTest() : StringSpec({

    "adding a frame" {
        val cache = StripPoolFrameCache()
        val frame = mockk<RenderedFrameModel>()
        val strip = mockk< LedStripModel>()
        val stripUuid = "ABC-XYZ"

        every { frame.sequenceNumber } returns 1
        every { strip.uuid } returns stripUuid
        every { frame.strip } returns strip

        cache.addFrameToCache(frame)
    }

    "sequence numbers" {
        val cache = StripPoolFrameCache()
        val frame = mockk<RenderedFrameModel>()
        val strip = mockk< LedStripModel>()
        val stripUuid = "ABC-XYZ"

        every { frame.sequenceNumber } returns 1
        every { strip.uuid } returns stripUuid
        every { frame.strip } returns strip

        var sequenceNumber = cache.getSequenceNumber(stripUuid)
        sequenceNumber shouldBe 1

        cache.addFrameToCache(frame)
        sequenceNumber = cache.getSequenceNumber(stripUuid)
        sequenceNumber shouldBe 2
    }

    "sequence numbers wrap at Short.MAX_VALUE" {
        val cache = StripPoolFrameCache()
        val frameA = mockk<RenderedFrameModel>()
        val frameB = mockk<RenderedFrameModel>()
        val frameC = mockk<RenderedFrameModel>()
        val frameD = mockk<RenderedFrameModel>()
        val strip = mockk< LedStripModel>()
        val stripUuid = "ABC-XYZ"

        every { strip.uuid } returns stripUuid
        every { frameA.sequenceNumber } returns (Short.MAX_VALUE - 1).toShort()
        every { frameA.strip } returns strip
        every { frameB.sequenceNumber } returns Short.MAX_VALUE
        every { frameB.strip } returns strip
        every { frameC.sequenceNumber } returns 1
        every { frameC.strip } returns strip
        every { frameD.sequenceNumber } returns 2
        every { frameD.strip } returns strip

        cache.addFrameToCache(frameA)
        var sequenceNumber = cache.getSequenceNumber(stripUuid)
        sequenceNumber shouldBe frameB.sequenceNumber

        cache.addFrameToCache(frameB)
        sequenceNumber = cache.getSequenceNumber(stripUuid)
        sequenceNumber shouldBe frameC.sequenceNumber

        cache.addFrameToCache(frameC)
        sequenceNumber = cache.getSequenceNumber(stripUuid)
        sequenceNumber shouldBe frameD.sequenceNumber

        cache.addFrameToCache(frameD)
        sequenceNumber = cache.getSequenceNumber(stripUuid)
        sequenceNumber shouldBe frameD.sequenceNumber + 1
    }
})