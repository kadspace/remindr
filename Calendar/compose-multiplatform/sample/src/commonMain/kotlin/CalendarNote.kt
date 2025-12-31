package com.kizitonwose.calendar.compose.multiplatform.sample

import androidx.compose.ui.graphics.Color
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.now
import com.kizitonwose.calendar.core.plusMonths
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atTime
import kotlinx.datetime.onDay

data class CalendarNote(
    val time: LocalDateTime,
    val text: String,
    val color: Color,
    val id: Long = -1,
)

fun generateNotes(): List<CalendarNote> = buildList {
    val currentMonth = YearMonth.now()

    currentMonth.onDay(17).also { date ->
        add(
            CalendarNote(
                date.atTime(14, 0),
                "Buy groceries",
                Color(0xFF1565C0),
            ),
        )
        add(
            CalendarNote(
                date.atTime(21, 30),
                "Call Mom",
                Color(0xFFC62828),
            ),
        )
    }

    currentMonth.onDay(22).also { date ->
        add(
            CalendarNote(
                date.atTime(13, 20),
                "Meeting with team",
                Color(0xFF5D4037),
            ),
        )
        add(
            CalendarNote(
                date.atTime(17, 40),
                "Gym session",
                Color(0xFF455A64),
            ),
        )
    }

    currentMonth.onDay(3).also { date ->
        add(
            CalendarNote(
                date.atTime(20, 0),
                "Dinner date",
                Color(0xFF00796B),
            ),
        )
    }

    currentMonth.onDay(12).also { date ->
        add(
            CalendarNote(
                date.atTime(18, 15),
                "Finish report",
                Color(0xFF0097A7),
            ),
        )
    }

    currentMonth.plusMonths(1).onDay(13).also { date ->
        add(
            CalendarNote(
                date.atTime(7, 30),
                "Doctor appointment",
                Color(0xFFC2185B),
            ),
        )
        add(
            CalendarNote(
                date.atTime(10, 50),
                "Car service",
                Color(0xFFEF6C00),
            ),
        )
    }

    currentMonth.minusMonths(1).onDay(9).also { date ->
        add(
            CalendarNote(
                date.atTime(20, 15),
                "Movie night",
                Color(0xFFEF6C00),
            ),
        )
    }
}
