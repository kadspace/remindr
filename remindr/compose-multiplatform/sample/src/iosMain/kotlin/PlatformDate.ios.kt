package com.kizitonwose.remindr.compose.multiplatform.sample

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSDate
import platform.Foundation.currentCalendar

actual fun getToday(): LocalDate {
    val date = NSDate()
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(unitFlags = NSCalendarUnitDay or NSCalendarUnitMonth or NSCalendarUnitYear, fromDate = date)
    return LocalDate(components.year.toInt(), components.month.toInt(), components.day.toInt())
}

actual fun getCurrentDateTime(): LocalDateTime {
    val date = NSDate()
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(
        unitFlags = NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond,
        fromDate = date
    )
    return LocalDateTime(
        components.year.toInt(),
        components.month.toInt(),
        components.day.toInt(),
        components.hour.toInt(),
        components.minute.toInt(),
        components.second.toInt()
    )
}
