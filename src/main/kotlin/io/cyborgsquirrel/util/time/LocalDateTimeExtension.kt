package io.cyborgsquirrel.util.time

import java.time.LocalDateTime
import java.time.ZoneId

fun LocalDateTime.millisSinceEpoch(): Long {
    return this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}