package com.kizitonwose.remindr.compose.multiplatform.sample

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDateTime

enum class Severity {
    LOW, MEDIUM, HIGH
}

data class CalendarNote(
    val time: LocalDateTime,
    val title: String, // New: Title
    val description: String?, // New: Description
    val endDate: LocalDateTime? = null, // New: End Date for ranges
    val color: Color,
    val id: Long = -1,
    val isCompleted: Boolean = false,
    val recurrenceType: String? = null,
    val recurrenceRule: String? = null, // Store RRule string if needed
    val nagEnabled: Boolean = false,
    val lastCompletedAt: LocalDateTime? = null,
    val snoozedUntil: LocalDateTime? = null,
    val severity: Severity = Severity.MEDIUM,
    val reminderOffsets: List<Long> = emptyList() // List of minutes
) {
    // Backwards compatibility for 'text' usage
    val text: String
        get() = if (description.isNullOrBlank()) title else "$title\n$description"
}
