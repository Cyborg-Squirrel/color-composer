package io.cyborgsquirrel.jobs.streaming.nightdriver

data class NightDriverSocketResponse(
    val size: Short, // 4 bytes
    val sequence: Int, // 8 bytes
    val flashVersion: Short, // 4 bytes
    val currentClock: Double, // 8 bytes - seconds since epoch
    val oldestPacket: Double, // 8 bytes - seconds relative to currentClock
    val newestPacket: Double, // 8 bytes - seconds relative to currentClock
    val brightness: Double, // 8 bytes
    val wifiSignal: Double, // 8 bytes
    val bufferSize: Short, // 4 bytes
    val bufferPosition: Short, // 4 bytes
    val frameDrawing: Short, // 4 bytes
    val watts: Short // 4 bytes
) {

    fun currentClockMillis() = (currentClock * 1000.0).toLong()

    fun oldestPacketMillis() = (oldestPacket * 1000.0).toLong()

    fun newestPacketMillis() = (newestPacket * 1000.0).toLong()

    companion object {
        const val SIZE_IN_BYTES = 72
    }
}