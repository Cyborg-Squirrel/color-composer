package io.cyborgsquirrel.util

import java.time.LocalDate

fun LocalDate.ymd(): String {
    val year = this.year
    val month = if (this.monthValue < 10) "0${this.monthValue}" else this.monthValue.toString()
    val day = if (this.dayOfMonth < 10) "0${this.dayOfMonth}" else this.dayOfMonth
    return "$year-$month-$day"
}