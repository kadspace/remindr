package com.remindr.app.util

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import com.kizitonwose.remindr.core.Week
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth

fun YearMonth.displayText(short: Boolean = false): String {
    return "${month.displayText(short = short)} $year"
}

fun Month.displayText(short: Boolean = true): String {
    return getDisplayName(short, enLocale)
}

fun DayOfWeek.displayText(uppercase: Boolean = false, narrow: Boolean = false): String {
    return getDisplayName(narrow, enLocale).let { value ->
        if (uppercase) value.toUpperCase(enLocale) else value
    }
}

expect fun Month.getDisplayName(short: Boolean, locale: Locale): String

expect fun DayOfWeek.getDisplayName(narrow: Boolean = false, locale: Locale): String

private val enLocale = Locale("en-US")

fun LocalDate.toUsDateString(): String {
    val monthText = monthNumber.toString().padStart(2, '0')
    val dayText = day.toString().padStart(2, '0')
    return "$monthText/$dayText/$year"
}

fun parseUsOrIsoLocalDateOrNull(input: String): LocalDate? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null

    runCatching { LocalDate.parse(trimmed) }.getOrNull()?.let { return it }

    val slashMatch = Regex("^(\\d{1,2})/(\\d{1,2})/(\\d{2}|\\d{4})$").find(trimmed) ?: return null
    val month = slashMatch.groupValues[1].toIntOrNull() ?: return null
    val day = slashMatch.groupValues[2].toIntOrNull() ?: return null
    val rawYear = slashMatch.groupValues[3].toIntOrNull() ?: return null
    val year = if (rawYear < 100) 2000 + rawYear else rawYear
    return runCatching { LocalDate(year, month, day) }.getOrNull()
}

fun getWeekPageTitle(week: Week): String {
    val firstDate = week.days.first().date
    val lastDate = week.days.last().date
    return when {
        firstDate.yearMonth == lastDate.yearMonth -> {
            firstDate.yearMonth.displayText()
        }
        firstDate.year == lastDate.year -> {
            "${firstDate.month.displayText(short = false)} - ${lastDate.yearMonth.displayText()}"
        }
        else -> {
            "${firstDate.yearMonth.displayText()} - ${lastDate.yearMonth.displayText()}"
        }
    }
}

fun formatTime12(time: LocalTime): String {
    return formatTime12(hour = time.hour, minute = time.minute)
}

fun formatTime12(hour: Int, minute: Int): String {
    val normalizedHour = hour.coerceIn(0, 23)
    val normalizedMinute = minute.coerceIn(0, 59)
    val meridiem = if (normalizedHour < 12) "AM" else "PM"
    val displayHour = when (val value = normalizedHour % 12) {
        0 -> 12
        else -> value
    }
    return "$displayHour:${normalizedMinute.toString().padStart(2, '0')} $meridiem"
}

fun formatTime24TextTo12(text: String): String {
    val match = Regex("^(\\d{1,2}):(\\d{2})$").find(text.trim()) ?: return text
    val hour = match.groupValues[1].toIntOrNull() ?: return text
    val minute = match.groupValues[2].toIntOrNull() ?: return text
    if (hour !in 0..23 || minute !in 0..59) return text
    return formatTime12(hour = hour, minute = minute)
}
