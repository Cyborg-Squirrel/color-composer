package io.cyborgsquirrel.lighting.rendering.cache

class ClientSequenceTracker {

    private val clientSequenceNumberMap = mutableMapOf<String, Map<String, Short>>()

    fun getSequenceNumber(clientUuid: String, stripUuid: String): Short {
        val sequenceNumber = clientSequenceNumberMap[clientUuid]?.get(stripUuid)
        return sequenceNumber ?: MIN_SEQUENCE_NUMBER
    }

    fun setSequenceNumber(clientUuid: String, stripPoolUuid: String, sequenceNumber: Short) {
        clientSequenceNumberMap[clientUuid] = mapOf(stripPoolUuid to sequenceNumber)
    }
}