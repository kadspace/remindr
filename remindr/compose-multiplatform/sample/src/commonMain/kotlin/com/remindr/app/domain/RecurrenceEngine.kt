package com.remindr.app.domain

import com.remindr.app.data.model.Item
import com.remindr.app.data.model.ItemStatus
import kotlinx.datetime.*

object RecurrenceEngine {

    fun getNextDueDate(item: Item): LocalDateTime? {
        if (item.recurrenceType == null || item.time == null) return null

        val lastInstance = item.lastCompletedAt ?: item.time

        val nextDate = when (item.recurrenceType) {
            "DAILY" -> lastInstance.date.plus(DatePeriod(days = 1)).atTime(item.time.time)
            "WEEKLY" -> lastInstance.date.plus(DatePeriod(days = 7)).atTime(item.time.time)
            "MONTHLY" -> lastInstance.date.plus(DatePeriod(months = 1)).atTime(item.time.time)
            "YEARLY" -> lastInstance.date.plus(DatePeriod(years = 1)).atTime(item.time.time)
            else -> return null
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
}
