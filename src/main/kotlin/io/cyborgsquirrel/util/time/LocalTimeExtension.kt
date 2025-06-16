package io.cyborgsquirrel.util.time

import java.time.LocalTime

fun LocalTime.secondsSinceMidnight(): Int {
    val hours = this.hour
    val minutes = this.minute
    val seconds = this.second
    return seconds + (minutes * 60) + (hours * 60 * 60)
}