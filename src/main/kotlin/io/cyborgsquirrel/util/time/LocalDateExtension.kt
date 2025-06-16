package io.cyborgsquirrel.util.time

import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun LocalDate.ymd(): String {
    val year = this.year
    val month = if (this.monthValue < 10) "0${this.monthValue}" else this.monthValue.toString()
    val day = if (this.dayOfMonth < 10) "0${this.dayOfMonth}" else this.dayOfMonth
    return "$year-$month-$day"
}

fun localDateFromYmd(ymd: String): LocalDate = LocalDate.parse(ymd, DateTimeFormatter.ISO_LOCAL_DATE)
