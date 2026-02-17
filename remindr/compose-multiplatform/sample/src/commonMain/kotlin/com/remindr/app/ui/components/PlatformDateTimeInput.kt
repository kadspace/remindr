package com.remindr.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformDateTimeInput(
    dueDate: String,
    onDueDateChange: (String) -> Unit,
    dueTime: String,
    onDueTimeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
)
