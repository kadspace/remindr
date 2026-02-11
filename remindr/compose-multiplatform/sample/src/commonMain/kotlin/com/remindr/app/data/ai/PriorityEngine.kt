package com.remindr.app.data.ai

import com.remindr.app.data.model.Item
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.data.model.ItemType
import com.remindr.app.data.model.Severity
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import kotlinx.datetime.daysUntil

data class PriorityItem(
    val item: Item,
    val score: Double,
    val context: String,
)

object PriorityEngine {

    fun getTopPriorities(items: List<Item>, today: LocalDate, limit: Int = 5): List<PriorityItem> {
        return items
            .filter { it.status != ItemStatus.COMPLETED && it.status != ItemStatus.ARCHIVED }
            .map { item -> scoreItem(item, today) }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun scoreItem(item: Item, today: LocalDate): PriorityItem {
        var score = 0.0
        var context = ""

        val itemDate = item.time?.date

        if (itemDate != null) {
            val daysUntilDue = today.daysUntil(itemDate)

            when {
                daysUntilDue < 0 -> {
                    // Overdue
                    score += 100.0 + (-daysUntilDue * 5)
                    val overdueDays = -daysUntilDue
                    context = if (overdueDays == 1) "overdue by 1 day" else "overdue by $overdueDays days"
                }
                daysUntilDue == 0 -> {
                    score += 90.0
                    context = "due today"
                }
                daysUntilDue in 1..3 -> {
                    score += 70.0 - (daysUntilDue * 5)
                    context = if (daysUntilDue == 1) "due tomorrow" else "due in $daysUntilDue days"
                }
                daysUntilDue in 4..7 -> {
                    score += 40.0 - (daysUntilDue * 2)
                    context = "due in $daysUntilDue days"
                }
                else -> {
                    score += 10.0
                    context = "due ${itemDate}"
                }
            }
        } else {
            // No date — score based on age and status
            score += 15.0
            context = "no due date"
        }

        // Status bonuses
        when (item.status) {
            ItemStatus.MONITORING -> {
                score += 20.0
                context = if (context.isNotEmpty()) "$context · monitoring" else "monitoring"
            }
            ItemStatus.IN_PROGRESS -> {
                score += 10.0
                context = if (context.isNotEmpty()) "$context · in progress" else "in progress"
            }
            else -> {}
        }

        // Severity bonus
        when (item.severity) {
            Severity.HIGH -> score += 15.0
            Severity.MEDIUM -> score += 5.0
            Severity.LOW -> score += 0.0
        }

        // Type bonus
        when (item.type) {
            ItemType.BILL -> score += 10.0
            ItemType.TASK -> score += 5.0
            ItemType.GOAL -> score += 3.0
            ItemType.RESEARCH -> score += 2.0
            ItemType.NOTE -> score += 0.0
        }

        return PriorityItem(item = item, score = score, context = context)
    }
}
