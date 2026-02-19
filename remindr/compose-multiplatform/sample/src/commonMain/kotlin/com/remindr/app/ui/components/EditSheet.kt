package com.remindr.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun EditSheet(
    title: String,
    onTitleChange: (String) -> Unit,
    dueSummary: String,
    recurrenceSummary: String?,
    scheduleModeSummary: String,
    willOccurPreview: String,
    dueDate: String,
    onDueDateChange: (String) -> Unit,
    dueDateError: String?,
    dueTime: String,
    onDueTimeChange: (String) -> Unit,
    dueTimeError: String?,
    scheduleMode: String,
    onScheduleModeChange: (String) -> Unit,
    recurrenceType: String?,
    onRecurrenceTypeChange: (String) -> Unit,
    recurrenceInterval: String,
    onRecurrenceIntervalChange: (String) -> Unit,
    endDate: String,
    onEndDateChange: (String) -> Unit,
    endDateError: String?,
    onSave: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .imePadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Edit reminder",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )

        Text(dueSummary, style = MaterialTheme.typography.bodyMedium)
        Text(scheduleModeSummary, style = MaterialTheme.typography.bodySmall)
        Text(recurrenceSummary ?: "No recurrence", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title") },
            singleLine = true,
        )

        PlatformDateTimeInput(
            dueDate = dueDate,
            onDueDateChange = onDueDateChange,
            dueTime = dueTime,
            onDueTimeChange = onDueTimeChange,
            modifier = Modifier.fillMaxWidth(),
        )
        if (dueDateError != null) {
            Text(
                text = dueDateError,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (dueTimeError != null) {
            Text(
                text = dueTimeError,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Text("Schedule", style = MaterialTheme.typography.labelSmall)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                ModeChip("One-time", scheduleMode == "ONE_TIME") { onScheduleModeChange("ONE_TIME") }
                ModeChip("Repeat forever", scheduleMode == "RECURRING_FOREVER") { onScheduleModeChange("RECURRING_FOREVER") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                ModeChip("Repeat until date", scheduleMode == "RECURRING_UNTIL_DATE") { onScheduleModeChange("RECURRING_UNTIL_DATE") }
            }
        }

        if (scheduleMode != "ONE_TIME") {
            Text("Frequency", style = MaterialTheme.typography.labelSmall)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    ModeChip("Daily", recurrenceType == "DAILY") { onRecurrenceTypeChange("DAILY") }
                    ModeChip("Weekly", recurrenceType == "WEEKLY") { onRecurrenceTypeChange("WEEKLY") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    ModeChip("Monthly", recurrenceType == "MONTHLY") { onRecurrenceTypeChange("MONTHLY") }
                    ModeChip("Yearly", recurrenceType == "YEARLY") { onRecurrenceTypeChange("YEARLY") }
                }
            }

            OutlinedTextField(
                value = recurrenceInterval,
                onValueChange = onRecurrenceIntervalChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Every N cycles") },
                placeholder = { Text("1") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            if (scheduleMode == "RECURRING_UNTIL_DATE") {
                PlatformDateInput(
                    value = endDate,
                    onValueChange = onEndDateChange,
                    label = "End date",
                    modifier = Modifier.fillMaxWidth(),
                )
                if (endDateError != null) {
                    Text(
                        text = endDateError,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Text(
            text = willOccurPreview,
            style = MaterialTheme.typography.bodySmall,
        )

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("Save", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}
