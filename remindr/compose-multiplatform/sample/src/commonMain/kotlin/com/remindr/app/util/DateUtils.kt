package com.remindr.app.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

expect fun getToday(): LocalDate
expect fun getCurrentDateTime(): LocalDateTime
expect fun getDateTimeAfterMinutes(minutes: Int): LocalDateTime

fun getFormattedTime(): String {
    val now = getCurrentDateTime()
    return "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}"
}
