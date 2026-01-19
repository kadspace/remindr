package com.kizitonwose.remindr.data

import com.kizitonwose.remindr.core.CalendarDay
import com.kizitonwose.remindr.core.DayPosition
import com.kizitonwose.remindr.core.nextMonth
import com.kizitonwose.remindr.core.previousMonth
import com.kizitonwose.remindr.core.yearMonth
import java.time.DayOfWeek
import java.time.YearMonth

// E.g DayOfWeek.SATURDAY.daysUntil(DayOfWeek.TUESDAY) = 3
public fun DayOfWeek.daysUntil(other: DayOfWeek): Int = (7 + (other.ordinal - ordinal)) % 7

// Find the actual month on the calendar where this date is shown.
public val CalendarDay.positionYearMonth: YearMonth
    get() = when (position) {
        DayPosition.InDate -> date.yearMonth.nextMonth
        DayPosition.MonthDate -> date.yearMonth
        DayPosition.OutDate -> date.yearMonth.previousMonth
    }

public inline fun <T> Iterable<T>.indexOfFirstOrNull(predicate: (T) -> Boolean): Int? {
    val result = indexOfFirst(predicate)
    return if (result == -1) null else result
}
