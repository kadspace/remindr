package com.remindr.app.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import java.time.LocalDate as JavaLocalDate
import java.time.LocalDateTime as JavaLocalDateTime

actual fun getToday(): LocalDate {
    val now = JavaLocalDate.now()
    return LocalDate(now.year, now.monthValue, now.dayOfMonth)
}

actual fun getCurrentDateTime(): LocalDateTime {
    val now = JavaLocalDateTime.now()
    return LocalDateTime(now.year, now.monthValue, now.dayOfMonth, now.hour, now.minute, now.second)
}

actual fun getDateTimeAfterMinutes(minutes: Int): LocalDateTime {
    val next = JavaLocalDateTime.now().plusMinutes(minutes.toLong())
    return LocalDateTime(next.year, next.monthValue, next.dayOfMonth, next.hour, next.minute, next.second)
}
