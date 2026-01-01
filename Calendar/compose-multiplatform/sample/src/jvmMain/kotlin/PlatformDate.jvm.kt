package com.kizitonwose.calendar.compose.multiplatform.sample

import kotlinx.datetime.LocalDate
import java.time.LocalDate as JavaLocalDate

actual fun getToday(): LocalDate {
    val now = JavaLocalDate.now()
    return LocalDate(now.year, now.monthValue, now.dayOfMonth)
}
