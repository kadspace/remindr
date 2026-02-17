package com.remindr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.remindr.app.data.model.Item
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.ui.theme.Colors

private val pageBackgroundColor: Color = Colors.example5PageBgColor
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(color = itemBackgroundColor)
                .fillMaxHeight()
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
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
        }

        Box(
            modifier = Modifier
                .background(
                    color = if (isDoneOrArchived) itemBackgroundColor.copy(alpha = 0.88f) else itemBackgroundColor,
                )
                .weight(1f)
                .fillMaxHeight()
                .padding(8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column {
                Text(
                    text = item.title,
                    color = titleColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                )
                Text(
                    text = item.dueSummary,
                    fontSize = 11.sp,
                    color = Colors.example5TextGreyLight,
                    modifier = Modifier.padding(top = 2.dp),
                )
                item.recurrenceSummary?.let { recurrence ->
                    Text(
                        text = recurrence,
                        fontSize = 10.sp,
                        color = Colors.example5TextGreyLight,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (item.status != ItemStatus.PENDING && item.status != ItemStatus.COMPLETED) {
                    Text(
                        text = item.status.name.replace("_", " "),
                        fontSize = 9.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .background(
                                Color.Gray.copy(alpha = 0.15f),
                                RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }

                if (item.snoozedUntil != null) {
                    Text(
                        text = "Snoozed until ${item.snoozedUntil.hour}:${item.snoozedUntil.minute.toString().padStart(2, '0')}",
                        fontSize = 10.sp,
                        color = Colors.example5TextGreyLight,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                if (item.status != ItemStatus.COMPLETED && item.status != ItemStatus.ARCHIVED) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TextButton(
                            onClick = { onSnooze(10) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        ) {
                            Text("Snooze 10m", fontSize = 11.sp, color = Colors.example5TextGreyLight)
                        }
                        TextButton(
                            onClick = { onSnooze(30) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        ) {
                            Text("Snooze 30m", fontSize = 11.sp, color = Colors.example5TextGreyLight)
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .background(color = itemBackgroundColor)
                .fillMaxHeight()
                .clickable(onClick = onArchive)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Archive,
                contentDescription = "Archive",
                tint = Color.Gray,
            )
        }
    }
    HorizontalDivider(thickness = 2.dp, color = pageBackgroundColor)
}
