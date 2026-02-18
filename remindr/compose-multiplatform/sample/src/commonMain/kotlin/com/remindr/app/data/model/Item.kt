package com.remindr.app.data.model

import androidx.compose.ui.graphics.Color
import com.remindr.app.util.formatTime12
import kotlinx.datetime.LocalDateTime

data class Item(
    val id: Long = -1,
    val title: String,
    val description: String? = null,
    val time: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val color: Color,
    val severity: Severity = Severity.MEDIUM,
    val type: ItemType = ItemType.TASK,
    val status: ItemStatus = ItemStatus.PENDING,
    val groupId: Long? = null,
    val parentId: Long? = null,
    val amount: Double? = null,
    val recurrenceType: String? = null,
    val recurrenceInterval: Int = 1,
    val recurrenceEndMode: String = "NEVER",
    val recurrenceEndDate: LocalDateTime? = null,
    val recurrenceRule: String? = null,
    val nagEnabled: Boolean = false,
    val lastCompletedAt: LocalDateTime? = null,
    val snoozedUntil: LocalDateTime? = null,
    val reminderOffsets: List<Long> = emptyList(),
    val createdAt: LocalDateTime,
) {
    val text: String
        get() = title

    val isCompleted: Boolean
        get() = status == ItemStatus.COMPLETED

    val dueSummary: String
        get() {
            val due = time ?: return "No due date"
            return "Due ${due.date} at ${formatTime12(due.time)}"
        }

    val recurrenceSummary: String?
        get() {
            val recurrence = recurrenceType?.uppercase() ?: return null
            val base = when (recurrence) {
                "DAILY" -> "Repeats daily"
                "WEEKLY" -> {
                    val day = time?.date?.dayOfWeek?.name?.toTitleCaseWord()
                    if (day != null) "Repeats weekly on $day" else "Repeats weekly"
                }
                "MONTHLY" -> {
                    val dayOfMonth = time?.date?.day
                    if (dayOfMonth != null) "Repeats monthly on day $dayOfMonth" else "Repeats monthly"
                }
                "YEARLY" -> {
                    val due = time
                    if (due != null) {
                        val month = due.date.month.name.toTitleCaseWord()
                        "Repeats yearly on $month ${due.date.day}"
                    } else {
                        "Repeats yearly"
                    }
                }
                else -> "Repeats ${recurrence.lowercase()}"
            }

            val endDate = recurrenceEndDate?.date ?: return base
            return "$base until $endDate"
        }

    val scheduleModeSummary: String
        get() {
            if (recurrenceType == null) return "One-time"
            return if (recurrenceEndMode == "UNTIL_DATE" && recurrenceEndDate != null) {
                "Recurring until ${recurrenceEndDate.date}"
            } else {
                "Recurring forever"
            }
        }
}

private fun String.toTitleCaseWord(): String {
    return lowercase().replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase() else char.toString()
    }
}
