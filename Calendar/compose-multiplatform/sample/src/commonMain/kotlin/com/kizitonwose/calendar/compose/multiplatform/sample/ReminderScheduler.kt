package com.kizitonwose.calendar.compose.multiplatform.sample

interface ReminderScheduler {
    fun schedule(note: CalendarNote)
    fun cancel(note: CalendarNote)
}
