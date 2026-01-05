package io.cyborgsquirrel.lighting.rendering.cache

import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameModel
import java.util.*
import java.util.concurrent.Semaphore

class StripPoolFrameCache {

    /**
     * Frame buffer. Used to avoid re-rendering effects for LED strip pools.
     */
    private var stripPoolFrames = mutableListOf<RenderedFrameModel>()
    private val lock = Semaphore(1)

    fun getFrameFromCache(stripUuid: String, sequenceNumber: Short): Optional<RenderedFrameModel> {
        lock.acquire()
        val matchingFramesForStrip = stripPoolFrames.filter { it.stripUuid == stripUuid }
        if (matchingFramesForStrip.isNotEmpty()) {
            val poolFrameSequenceNumbers = mutableSetOf<Short>()
            if (matchingFramesForStrip.size > 2) {
                val sequenceNumbersSorted = matchingFramesForStrip.map { it.sequenceNumber }.sortedDescending()
                val sequenceNumbersToPrune = mutableSetOf<Short>()
                poolFrameSequenceNumbers.addAll(sequenceNumbersSorted - sequenceNumbersToPrune)
                for (i in 0..<sequenceNumbersSorted.size - 1) {
                    // Handle wrap scenario (1 is greater than Short.MAX_VALUE in this case)
                    if (sequenceNumbersSorted[i] - sequenceNumbersSorted[i + 1] > 1) {
                        sequenceNumbersToPrune.add(sequenceNumbersSorted[i])
                    } else if (i > 1) {
                        // i = 2 or more, we have enough buffered frames and can remove the rest
                        sequenceNumbersToPrune.add(sequenceNumbersSorted[i])
                    }
                }

                stripPoolFrames.removeIf { it.stripUuid == stripUuid && sequenceNumbersToPrune.contains(it.sequenceNumber) }
            } else {
                poolFrameSequenceNumbers.addAll(matchingFramesForStrip.map { it.sequenceNumber })
            }

            // sequenceNumber 0 means the caller is making its first call. Return the latest frame.
            val frame = if (sequenceNumber <= 0) {
                matchingFramesForStrip.maxByOrNull { it.sequenceNumber }!!
            } else if (poolFrameSequenceNumbers.contains(sequenceNumber)) {
                matchingFramesForStrip.first { it.sequenceNumber == sequenceNumber }
            } else {
                null
            }

            lock.release()
            return if (frame != null) {
                Optional.of(frame)
            } else {
                Optional.empty()
            }
        }

        lock.release()
        return Optional.empty()
    }

    fun addFrameToCache(frame: RenderedFrameModel) {
        if (frame.sequenceNumber < 1) {
            throw Exception("Invalid frame sequence number ${frame.sequenceNumber}")
        }

        lock.acquire()
        stripPoolFrames.add(frame)
        pruneFrames(frame.stripUuid)
        lock.release()
    }

    fun getSequenceNumber(stripUuid: String): Short {
        lock.acquire()
        val frames = stripPoolFrames.filter { it.stripUuid == stripUuid }.sortedBy { it.sequenceNumber }
        lock.release()
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
        val matchingFramesForStrip = stripPoolFrames.filter { it.stripUuid == stripUuid }
        if (matchingFramesForStrip.size > MAX_BUFFER_SIZE_PER_STRIP) {
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
                    done = i >= sequenceNumbersSorted.size - 1 || (sequenceNumbersSorted.size - sequenceNumbersToPrune.size) == MAX_BUFFER_SIZE_PER_STRIP
                    i++
                }
            } else {
                sequenceNumbersToPrune.addAll(sequenceNumbersSorted.subList(MAX_BUFFER_SIZE_PER_STRIP - 1, sequenceNumbersSorted.size))
            }

            stripPoolFrames.removeIf { it.stripUuid == stripUuid && sequenceNumbersToPrune.contains(it.sequenceNumber) }
        }
    }

    companion object {
        private const val MIN_SEQUENCE_NUMBER: Short = 1
        private const val MAX_BUFFER_SIZE_PER_STRIP = 2
    }
}