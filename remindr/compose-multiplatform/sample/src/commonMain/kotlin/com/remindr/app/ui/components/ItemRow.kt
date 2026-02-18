package com.remindr.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.remindr.app.data.model.Item
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.ui.theme.Colors
import com.remindr.app.util.formatTime12

private val itemBackgroundColor: Color = Colors.example5ItemViewBgColor

@Composable
fun LazyItemScope.ItemRow(
    item: Item,
    onArchive: () -> Unit,
    onStatusChange: (ItemStatus) -> Unit,
    onSnooze: (Int) -> Unit,
) {
    val isCompleted = item.isCompleted
    val isDoneOrArchived = item.status == ItemStatus.COMPLETED || item.status == ItemStatus.ARCHIVED
    val titleColor = if (isDoneOrArchived) Colors.reminderDoneGray else Colors.reminderActiveRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 3.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDoneOrArchived) itemBackgroundColor.copy(alpha = 0.82f) else itemBackgroundColor.copy(alpha = 0.9f),
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { checked ->
                    onStatusChange(if (checked) ItemStatus.COMPLETED else ItemStatus.PENDING)
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = Colors.accent,
                    uncheckedColor = Color.Gray,
                    checkmarkColor = Color.White,
                ),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = item.title,
                    color = titleColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                )
                Text(
                    text = item.dueSummary,
                    fontSize = 11.sp,
                    color = Colors.example5TextGreyLight,
                )
                item.recurrenceSummary?.let { recurrence ->
                    Text(
                        text = recurrence,
                        fontSize = 11.sp,
                        color = Colors.example5TextGreyLight,
                    )
                }

                if (item.snoozedUntil != null) {
                    Text(
                        text = "Snoozed until ${formatTime12(item.snoozedUntil.time)}",
                        fontSize = 11.sp,
                        color = Colors.example5TextGreyLight,
                    )
                }

                if (!isDoneOrArchived) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = { onSnooze(10) },
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                        ) {
                            Text("Snooze 10m", fontSize = 11.sp, color = Colors.example5TextGreyLight)
                        }
                        TextButton(
                            onClick = { onSnooze(30) },
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                        ) {
                            Text("Snooze 30m", fontSize = 11.sp, color = Colors.example5TextGreyLight)
                        }
                    }
                }
            }

            IconButton(
                onClick = onArchive,
                modifier = Modifier.padding(top = 1.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = "Archive",
                    tint = Color.Gray,
                )
            }
        }
    }
}
