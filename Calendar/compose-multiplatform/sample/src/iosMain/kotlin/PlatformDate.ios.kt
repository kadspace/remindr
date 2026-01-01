package com.kizitonwose.calendar.compose.multiplatform.sample

import kotlinx.datetime.LocalDate
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.currentCalendar

actual fun getToday(): LocalDate {
    val date = NSDate()
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(unitFlags = NSCalendarUnitDay or NSCalendarUnitMonth or NSCalendarUnitYear, fromDate = date)
    return LocalDate(components.year.toInt(), components.month.toInt(), components.day.toInt())
}
