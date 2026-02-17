package com.remindr.app.domain

import com.remindr.app.data.model.Item
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.util.getCurrentDateTime
import kotlinx.datetime.*

object RecurrenceEngine {

    fun getNextDueDate(item: Item): LocalDateTime? {
        if (item.recurrenceType == null || item.time == null) return null

        val recurrenceType = item.recurrenceType.uppercase()
        val now = getCurrentDateTime()
        var nextDate = advance(item.time, recurrenceType) ?: return null

        // Keep recurring anchors stable (e.g. monthly on the 1st) and roll forward to future.
        while (nextDate <= now) {
            nextDate = advance(nextDate, recurrenceType) ?: return null
        }

        // Check recurrence end date
        if (item.recurrenceEndDate != null && nextDate > item.recurrenceEndDate) {
            return null
        }

        return nextDate
    }

    fun shouldNag(item: Item, now: LocalDateTime): Boolean {
        if (!item.nagEnabled) return false
        if (item.status == ItemStatus.COMPLETED) return false
        if (item.time == null) return false
        return now > item.time
    }

    private fun advance(from: LocalDateTime, recurrenceType: String): LocalDateTime? {
        return when (recurrenceType) {
            "DAILY" -> from.date.plus(DatePeriod(days = 1)).atTime(from.time)
            "WEEKLY" -> from.date.plus(DatePeriod(days = 7)).atTime(from.time)
            "MONTHLY" -> from.date.plus(DatePeriod(months = 1)).atTime(from.time)
            "YEARLY" -> from.date.plus(DatePeriod(years = 1)).atTime(from.time)
            else -> null
        }
    }
}
