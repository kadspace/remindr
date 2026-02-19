package com.remindr.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remindr.app.data.model.Item
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.ui.theme.Colors

private val itemBackgroundColor: Color = Colors.example5ItemViewBgColor

@Composable
fun ReminderRow(
    item: Item,
    onStatusChange: (ItemStatus) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(item.id, item.status) { mutableStateOf(false) }
    val isCompleted = item.status == ItemStatus.COMPLETED
    val isArchived = item.status == ItemStatus.ARCHIVED
    val isDeleted = item.status == ItemStatus.DELETED
    val isDoneState = isCompleted || isArchived || isDeleted
    val canToggleCheckbox = !isArchived && !isDeleted
    val titleColor = if (isDoneState) Colors.reminderDoneGray else Colors.reminderActiveRed

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = itemBackgroundColor.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(start = 4.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isCompleted,
                    enabled = canToggleCheckbox,
                    onCheckedChange = { checked ->
                        onStatusChange(if (checked) ItemStatus.COMPLETED else ItemStatus.PENDING)
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Colors.accent,
                        uncheckedColor = Color.Gray,
                        checkmarkColor = Color.White,
                        disabledUncheckedColor = Color.Gray.copy(alpha = 0.45f),
                    ),
                )
                Text(
                    text = item.title,
                    color = titleColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Colors.example5TextGreyLight,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.01f))
                        .clickable(onClick = onEdit)
                        .padding(start = 52.dp, end = 10.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.dueSummary,
                        fontSize = 12.sp,
                        color = Colors.example5TextGreyLight,
                    )
                    item.recurrenceSummary?.let { recurrence ->
                        Text(
                            text = recurrence,
                            fontSize = 12.sp,
                            color = Colors.example5TextGreyLight,
                        )
                    }
                }
            }
        }
    }
}
