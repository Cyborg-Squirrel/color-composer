package io.cyborgsquirrel.lighting.rendering.cache

import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameModel

class StripPoolFrameCache {

    /**
     * Frame buffer. Used to avoid re-rendering effects for LED strip pools.
     */
    private val stripPoolFrames = mutableListOf<RenderedFrameModel>()

    fun getFrameFromCache(stripUuid: String, sequenceNumber: Short): RenderedFrameModel? {
        val matchingFramesForStrip = stripPoolFrames.filter { it.strip.uuid == stripUuid }

        // sequenceNumber 0 means the caller is making its first call. Return the latest frame.
        val frame = if (sequenceNumber <= 0) {
            matchingFramesForStrip.maxByOrNull { it.sequenceNumber }!!
        } else if (matchingFramesForStrip.map { it.sequenceNumber }.contains(sequenceNumber)) {
            matchingFramesForStrip.first { it.sequenceNumber == sequenceNumber }
        } else {
            null
        }

        return frame
    }

    fun addFrameToCache(frame: RenderedFrameModel) {
        if (frame.sequenceNumber < 1) {
            throw Exception("Invalid frame sequence number ${frame.sequenceNumber}")
        }

        stripPoolFrames.add(frame)
        pruneFrames(frame.strip.uuid)
    }

    fun getSequenceNumber(stripUuid: String): Short {
        val frames = stripPoolFrames.filter { it.strip.uuid == stripUuid }.sortedBy { it.sequenceNumber }
        if (frames.isEmpty()) {
            return MIN_SEQUENCE_NUMBER
        } else {
            val sequenceNumbers = frames.map { it.sequenceNumber }
            if (sequenceNumbers.last() == Short.MAX_VALUE) {
                if (sequenceNumbers.contains(MIN_SEQUENCE_NUMBER)) {
                    for (i in 0..<sequenceNumbers.size - 1) {
                        if (sequenceNumbers[i + 1] > sequenceNumbers[i] + 1) {
                            return (sequenceNumbers[i] + 1).toShort()
                        }
                    }
                } else {
                    return MIN_SEQUENCE_NUMBER
                }
            } else {
                return (frames.last().sequenceNumber + 1).toShort()
            }
        }

        throw Exception("Error determining sequence number for strip $stripUuid")
    }

    private fun pruneFrames(stripUuid: String) {
        val matchingFramesForStrip = stripPoolFrames.filter { it.strip.uuid == stripUuid }
        if (matchingFramesForStrip.size > MAX_CACHE_SIZE_PER_STRIP) {
            val sequenceNumbersSorted = matchingFramesForStrip.map { it.sequenceNumber }.sorted()
            val sequenceNumbersToPrune = mutableSetOf<Short>()
            if (sequenceNumbersSorted.last() == Short.MAX_VALUE) {
                var i = 0
                var done = false
                var didReachSequenceBreak = !sequenceNumbersSorted.contains(MIN_SEQUENCE_NUMBER)
                while (!done) {
                    val sequential = if (i < sequenceNumbersSorted.size - 1) {
                        sequenceNumbersSorted[i + 1] - sequenceNumbersSorted[i] == 1
                    } else {
                        sequenceNumbersSorted[i] - sequenceNumbersSorted[i - 1] == 1
                    }

                    if (didReachSequenceBreak) {
                        sequenceNumbersToPrune.add(sequenceNumbersSorted[i])
                    }

                    didReachSequenceBreak = didReachSequenceBreak || !sequential
                    done =
                        i >= sequenceNumbersSorted.size - 1 || (sequenceNumbersSorted.size - sequenceNumbersToPrune.size) == MAX_CACHE_SIZE_PER_STRIP
                    i++
                }
            } else {
                sequenceNumbersToPrune.addAll(
                    sequenceNumbersSorted.subList(
                        MAX_CACHE_SIZE_PER_STRIP - 1,
                        sequenceNumbersSorted.size
                    )
                )
            }

            stripPoolFrames.removeIf { it.strip.uuid == stripUuid && sequenceNumbersToPrune.contains(it.sequenceNumber) }
        }
    }

    companion object {
        private const val MAX_CACHE_SIZE_PER_STRIP = 2
    }
}