package com.kizitonwose.remindr.view.internal.monthcalendar

import com.kizitonwose.remindr.core.CalendarDay
import com.kizitonwose.remindr.view.CalendarView
import com.kizitonwose.remindr.view.LayoutHelper
import com.kizitonwose.remindr.view.MarginValues
import com.kizitonwose.remindr.view.internal.CalendarLayoutManager
import com.kizitonwose.remindr.view.internal.dayTag
import java.time.YearMonth

internal class MonthCalendarLayoutManager(private val calView: CalendarView) :
    CalendarLayoutManager<YearMonth, CalendarDay>(calView, calView.orientation) {
    private val adapter: MonthCalendarAdapter
        get() = calView.adapter as MonthCalendarAdapter

    override fun getaItemAdapterPosition(data: YearMonth): Int = adapter.getAdapterPosition(data)
    override fun getaDayAdapterPosition(data: CalendarDay): Int = adapter.getAdapterPosition(data)
    override fun getDayTag(data: CalendarDay): Int = dayTag(data.date)
    override fun getItemMargins(): MarginValues = calView.monthMargins
    override fun scrollPaged(): Boolean = calView.scrollPaged
    override fun notifyScrollListenerIfNeeded() = adapter.notifyMonthScrollListenerIfNeeded()
    override fun getLayoutHelper(): LayoutHelper? = calView.layoutHelper
}
