package com.remindr.app.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.remindr.app.util.formatTime24TextTo12
import kotlinx.datetime.LocalDate
import java.util.Calendar
import java.util.Locale

@Composable
actual fun PlatformDateTimeInput(
    dueDate: String,
    onDueDateChange: (String) -> Unit,
    dueTime: String,
    onDueTimeChange: (String) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = {
                val initialDate = runCatching { LocalDate.parse(dueDate) }.getOrNull()
                    ?: run {
                        val now = Calendar.getInstance()
                        LocalDate(
                            year = now.get(Calendar.YEAR),
                            monthNumber = now.get(Calendar.MONTH) + 1,
                            dayOfMonth = now.get(Calendar.DAY_OF_MONTH),
                        )
                    }
                DatePickerDialog(
                    context,
                    { _, year, monthOfYear, dayOfMonth ->
                        onDueDateChange(
                            String.format(
                                Locale.US,
                                "%04d-%02d-%02d",
                                year,
                                monthOfYear + 1,
                                dayOfMonth,
                            )
                        )
                    },
                    initialDate.year,
                    initialDate.monthNumber - 1,
                    initialDate.day,
                ).show()
            },
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null)
            Text(
                text = if (dueDate.isBlank()) "Pick date" else dueDate,
                modifier = Modifier.padding(start = 6.dp),
            )
        }

        OutlinedButton(
            onClick = {
                val timeMatch = Regex("^(\\d{1,2}):(\\d{2})$").find(dueTime)
                val initialHour = timeMatch?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
                val initialMinute = timeMatch?.groupValues?.getOrNull(2)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        onDueTimeChange(
                            String.format(
                                Locale.US,
                                "%02d:%02d",
                                hourOfDay,
                                minute,
                            )
                        )
                    },
                    initialHour,
                    initialMinute,
                    false,
                ).show()
            },
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.AccessTime, contentDescription = null)
            Text(
                text = if (dueTime.isBlank()) "Pick time" else formatTime24TextTo12(dueTime),
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}
