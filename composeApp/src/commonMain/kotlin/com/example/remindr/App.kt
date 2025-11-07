package com.example.remindr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Example3Page()
        }
    }
}

@Composable
fun Example3Page(modifier: Modifier = Modifier) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var weekStart by remember { mutableStateOf(today.startOfWeek()) }
    var selectedDate by remember { mutableStateOf(today) }

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Weekly agenda",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = {
                val currentDayOffset = selectedDate.dayOfWeek.ordinal
                weekStart = weekStart.minus(1, DateTimeUnit.WEEK)
                selectedDate = weekStart.plus(currentDayOffset, DateTimeUnit.DAY)
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous week")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = weekStart.formatMonthRange(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Week of ${weekStart.dayOfMonth}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = {
                val currentDayOffset = selectedDate.dayOfWeek.ordinal
                weekStart = weekStart.plus(1, DateTimeUnit.WEEK)
                selectedDate = weekStart.plus(currentDayOffset, DateTimeUnit.DAY)
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next week")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            for (index in 0..6) {
                val date = weekStart.plus(index, DateTimeUnit.DAY)
                DayChip(
                    date = date,
                    isSelected = date == selectedDate,
                    isToday = date == today,
                    onClick = { selectedDate = date }
                )
            }
        }

        Divider()

        val events = remember(selectedDate) { sampleEvents[selectedDate].orEmpty() }
        if (events.isEmpty()) {
            EmptyEventsState(selectedDate)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(events) { event ->
                    EventCard(event = event)
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayChip(date: LocalDate, isSelected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .background(background)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun EmptyEventsState(date: LocalDate) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "No meetings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "You're all clear on ${date.formatLong()}.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EventCard(event: Meeting) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = event.timeRange, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (event.notes != null) {
                Text(
                    text = event.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class Meeting(
    val date: LocalDate,
    val title: String,
    val startHour: Int,
    val startMinute: Int,
    val durationMinutes: Int,
    val notes: String? = null
) {
    val timeRange: String
        get() {
            val start = "%02d:%02d".format(startHour, startMinute)
            val endTotal = startHour * 60 + startMinute + durationMinutes
            val endHour = endTotal / 60
            val endMinute = endTotal % 60
            val end = "%02d:%02d".format(endHour, endMinute)
            return "$start – $end"
        }
}

private val sampleEvents: Map<LocalDate, List<Meeting>> = buildMap {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    fun LocalDate.addDays(days: Int) = this.plus(days, DateTimeUnit.DAY)

    put(today, listOf(
        Meeting(date = today, title = "Design sync", startHour = 9, startMinute = 0, durationMinutes = 60, notes = "Discuss new onboarding flow."),
        Meeting(date = today, title = "Product review", startHour = 13, startMinute = 30, durationMinutes = 45),
        Meeting(date = today, title = "Yoga", startHour = 18, startMinute = 0, durationMinutes = 60, notes = "Studio class downtown."),
    ))

    put(today.addDays(1), listOf(
        Meeting(date = today.addDays(1), title = "Team stand-up", startHour = 10, startMinute = 0, durationMinutes = 30),
        Meeting(date = today.addDays(1), title = "Client call", startHour = 16, startMinute = 15, durationMinutes = 45, notes = "Review contract changes."),
    ))

    put(today.addDays(2), listOf(
        Meeting(date = today.addDays(2), title = "Engineering retro", startHour = 11, startMinute = 0, durationMinutes = 60),
    ))

    put(today.addDays(4), listOf(
        Meeting(date = today.addDays(4), title = "One-on-one", startHour = 14, startMinute = 30, durationMinutes = 45, notes = "Performance check-in."),
        Meeting(date = today.addDays(4), title = "Dinner with friends", startHour = 19, startMinute = 0, durationMinutes = 120),
    ))
}

private fun LocalDate.startOfWeek(): LocalDate {
    val targetWeekStart = DayOfWeek.MONDAY
    var date = this
    while (date.dayOfWeek != targetWeekStart) {
        date = date.minus(1, DateTimeUnit.DAY)
    }
    return date
}

private fun LocalDate.formatMonthRange(): String {
    val endOfWeek = this.plus(6, DateTimeUnit.DAY)
    return if (this.month == endOfWeek.month) {
        "${this.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${this.year}"
    } else {
        val startMonth = this.month.name.lowercase().replaceFirstChar { it.uppercase() }
        val endMonth = endOfWeek.month.name.lowercase().replaceFirstChar { it.uppercase() }
        "$startMonth – $endMonth ${endOfWeek.year}"
    }
}

private fun LocalDate.formatLong(): String {
    val monthName = month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$monthName ${dayOfMonth}, ${year}"
}
