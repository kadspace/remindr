package com.kizitonwose.remindr.compose.multiplatform.sample

import kotlinx.datetime.*

object RecurrenceEngine {

    fun getNextDueDate(note: CalendarNote): LocalDateTime? {
        if (note.recurrenceType == null) return null
        
        // Basic implementation for now:
        // If "complete" and "nag", when should it show up again?
        // Or if just "recurring", when is the next instance?
        
        val lastInstance = note.lastCompletedAt ?: note.time
        
        return when (note.recurrenceType) {
            "DAILY" -> lastInstance.date.plus(DatePeriod(days = 1)).atTime(note.time.time)
            "WEEKLY" -> lastInstance.date.plus(DatePeriod(days = 7)).atTime(note.time.time)
            "MONTHLY" -> lastInstance.date.plus(DatePeriod(months = 1)).atTime(note.time.time)
            "YEARLY" -> lastInstance.date.plus(DatePeriod(years = 1)).atTime(note.time.time)
            else -> null
        }
    }

    fun shouldNag(note: CalendarNote, now: LocalDateTime): Boolean {
        if (!note.nagEnabled) return false
        if (note.isCompleted) return false
        
        // If it's overdue, nag!
        return now > note.time
    }
}
