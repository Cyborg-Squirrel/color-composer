package io.cyborgsquirrel.jobs.streaming.nightdriver

data class NightDriverTcpResponse(
    val size: Short,
    val sequence: Int,
    val flashVersion: Int,
    val currentClock: Double,
    val oldestPacket: Double,
    val newestPacket: Double,
    val brightness: Double,
    val wifiSignal: Double,
    val bufferSize: Int,
    val bufferPosition: Int,
    val frameDrawing: Int,
    val watts: Int
)