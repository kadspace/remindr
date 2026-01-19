package com.kizitonwose.remindr.view.internal.weekcalendar

import com.kizitonwose.remindr.view.LayoutHelper
import com.kizitonwose.remindr.view.MarginValues
import com.kizitonwose.remindr.view.WeekCalendarView
import com.kizitonwose.remindr.view.internal.CalendarLayoutManager
import com.kizitonwose.remindr.view.internal.dayTag
import java.time.LocalDate

internal class WeekCalendarLayoutManager(private val calView: WeekCalendarView) :
    CalendarLayoutManager<LocalDate, LocalDate>(calView, HORIZONTAL) {
    private val adapter: WeekCalendarAdapter
        get() = calView.adapter as WeekCalendarAdapter

    override fun getaItemAdapterPosition(data: LocalDate): Int = adapter.getAdapterPosition(data)
    override fun getaDayAdapterPosition(data: LocalDate): Int = adapter.getAdapterPosition(data)
    override fun getDayTag(data: LocalDate): Int = dayTag(data)
    override fun getItemMargins(): MarginValues = calView.weekMargins
    override fun scrollPaged(): Boolean = calView.scrollPaged
    override fun notifyScrollListenerIfNeeded() = adapter.notifyWeekScrollListenerIfNeeded()
    override fun getLayoutHelper(): LayoutHelper? = calView.layoutHelper
}
