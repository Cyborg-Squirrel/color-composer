package io.cyborgsquirrel.lighting.rendering.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ClientSequenceTrackerTest : StringSpec({

    "returns the minimum sequence number when nothing is tracked" {
        val tracker = ClientSequenceTracker()
        tracker.getSequenceNumber("client-a", "strip-1") shouldBe MIN_SEQUENCE_NUMBER
    }

    "tracks the sequence number for a strip" {
        val tracker = ClientSequenceTracker()
        tracker.setSequenceNumber("client-a", "strip-1", 5.toShort())
        tracker.getSequenceNumber("client-a", "strip-1") shouldBe 5.toShort()
    }

    "a client driving multiple pools does not clobber the other pools" {
        val tracker = ClientSequenceTracker()
        tracker.setSequenceNumber("client-a", "pool-1", 7.toShort())
        tracker.setSequenceNumber("client-a", "pool-2", 11.toShort())

        // Before the fix, setting pool-2 replaced the whole inner map and reset pool-1 to the minimum.
        tracker.getSequenceNumber("client-a", "pool-1") shouldBe 7.toShort()
        tracker.getSequenceNumber("client-a", "pool-2") shouldBe 11.toShort()
    }

    "sequence numbers are isolated per client" {
        val tracker = ClientSequenceTracker()
        tracker.setSequenceNumber("client-a", "strip-1", 3.toShort())
        tracker.setSequenceNumber("client-b", "strip-1", 9.toShort())

        tracker.getSequenceNumber("client-a", "strip-1") shouldBe 3.toShort()
        tracker.getSequenceNumber("client-b", "strip-1") shouldBe 9.toShort()
    }
})
