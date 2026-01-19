package com.kizitonwose.remindr.utils

import com.kizitonwose.remindr.core.OutDateStyle
import com.kizitonwose.remindr.core.minusMonths
import com.kizitonwose.remindr.core.plusMonths
import com.kizitonwose.remindr.data.getCalendarMonthData
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.YearMonth

internal infix fun <A, B, C> Pair<A, B>.toTriple(that: C): Triple<A, B, C> = Triple(first, second, that)

internal fun YearMonth.weeksInMonth(firstDayOfWeek: DayOfWeek) = getCalendarMonthData(
    startMonth = this,
    offset = 0,
    firstDayOfWeek = firstDayOfWeek,
    outDateStyle = OutDateStyle.EndOfRow,
).calendarMonth.weekDays.count()

internal val YearMonth.nextMonth: YearMonth
    get() = this.plusMonths(1)

internal val YearMonth.previousMonth: YearMonth
    get() = this.minusMonths(1)
