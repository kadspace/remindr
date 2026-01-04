package com.kizitonwose.calendar.compose.multiplatform.sample

import androidx.compose.ui.graphics.Color
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.now
import com.kizitonwose.calendar.core.plusMonths
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atTime
import kotlinx.datetime.onDay

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

fun generateNotes(): List<CalendarNote> = buildList {
    val currentMonth = YearMonth.now()

    currentMonth.onDay(17).also { date ->
        add(
            CalendarNote(
                time = date.atTime(14, 0),
                title = "Buy groceries",
                description = "Milk, Eggs, Bread",
                color = Color(0xFF1565C0),
            ),
        )
        add(
            CalendarNote(
                time = date.atTime(21, 30),
                title = "Call Mom",
                description = null,
                color = Color(0xFFC62828),
            ),
        )
    }

    currentMonth.onDay(22).also { date ->
        add(
            CalendarNote(
                time = date.atTime(13, 20),
                title = "Meeting with team",
                description = "Discuss Q1 goals",
                color = Color(0xFF5D4037),
            ),
        )
        add(
            CalendarNote(
                time = date.atTime(17, 40),
                title = "Gym session",
                description = "Leg day",
                color = Color(0xFF455A64),
            ),
        )
    }

    currentMonth.onDay(3).also { date ->
        add(
            CalendarNote(
                time = date.atTime(20, 0),
                title = "Dinner date",
                description = "Italian place",
                color = Color(0xFF00796B),
            ),
        )
    }

    currentMonth.onDay(12).also { date ->
        add(
            CalendarNote(
                time = date.atTime(18, 15),
                title = "Finish report",
                description = null,
                color = Color(0xFF0097A7),
            ),
        )
    }

    currentMonth.plusMonths(1).onDay(13).also { date ->
        add(
            CalendarNote(
                time = date.atTime(7, 30),
                title = "Doctor appointment",
                description = "Annual checkup",
                color = Color(0xFFC2185B),
            ),
        )
        add(
            CalendarNote(
                time = date.atTime(10, 50),
                title = "Car service",
                description = "Oil change",
                color = Color(0xFFEF6C00),
            ),
        )
    }

    currentMonth.minusMonths(1).onDay(9).also { date ->
        add(
            CalendarNote(
                time = date.atTime(20, 15),
                title = "Movie night",
                description = null,
                color = Color(0xFFEF6C00),
            ),
        )
    }
}
