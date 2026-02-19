package com.remindr.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun PlatformDateTimeInput(
    dueDate: String,
    onDueDateChange: (String) -> Unit,
    dueTime: String,
    onDueTimeChange: (String) -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = dueDate,
            onValueChange = onDueDateChange,
            modifier = Modifier.weight(1f),
            label = { Text("Due Date (MM/DD/YYYY)") },
            singleLine = true,
        )
        OutlinedTextField(
            value = dueTime,
            onValueChange = onDueTimeChange,
            modifier = Modifier.width(140.dp),
            label = { Text("Time (HH:MM)") },
            singleLine = true,
        )
    }
}
