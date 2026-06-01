package io.cyborgsquirrel.lighting.rendering.cache

import java.util.concurrent.ConcurrentHashMap

class ClientSequenceTracker {

    // clientUuid -> (stripUuid -> sequence number). Concurrent maps because independent pool jobs for the same
    // client may read/write different strips' sequence numbers at the same time.
    private val clientSequenceNumberMap = ConcurrentHashMap<String, ConcurrentHashMap<String, Short>>()

    fun getSequenceNumber(clientUuid: String, stripUuid: String): Short {
        val sequenceNumber = clientSequenceNumberMap[clientUuid]?.get(stripUuid)
        return sequenceNumber ?: MIN_SEQUENCE_NUMBER
    }

    fun setSequenceNumber(clientUuid: String, stripPoolUuid: String, sequenceNumber: Short) {
        // Update only this strip's entry so a client driving multiple pools doesn't clobber the others.
        clientSequenceNumberMap.getOrPut(clientUuid) { ConcurrentHashMap() }[stripPoolUuid] = sequenceNumber
    }
}