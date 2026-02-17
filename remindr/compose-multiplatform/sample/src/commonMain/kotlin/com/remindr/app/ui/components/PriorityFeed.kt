package com.remindr.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remindr.app.data.ai.PriorityItem
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.ui.theme.Colors

private val itemBackgroundColor: Color = Colors.example5ItemViewBgColor

@Composable
fun PriorityFeed(
    priorities: List<PriorityItem>,
    onStatusChange: (Long, ItemStatus) -> Unit,
    onSnooze: (Long, Int) -> Unit,
    onItemClick: (Long) -> Unit,
) {
    if (priorities.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        priorities.forEach { priority ->
            val item = priority.item
            val isDoneOrArchived = item.status == ItemStatus.COMPLETED || item.status == ItemStatus.ARCHIVED
            val titleColor = if (isDoneOrArchived) Colors.reminderDoneGray else Colors.reminderActiveRed

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item.id) },
                colors = CardDefaults.cardColors(containerColor = itemBackgroundColor),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = item.isCompleted,
                        onCheckedChange = { checked ->
                            onStatusChange(item.id, if (checked) ItemStatus.COMPLETED else ItemStatus.PENDING)
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
                            .padding(start = 4.dp),
                    ) {
                        Text(
                            text = item.title,
                            color = titleColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = priority.context,
                            color = Colors.example5TextGreyLight,
                            fontSize = 12.sp,
                        )
                        item.recurrenceSummary?.let { recurrence ->
                            Text(
                                text = recurrence,
                                color = Colors.example5TextGreyLight,
                                fontSize = 11.sp,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TextButton(
                            onClick = { onSnooze(item.id, 10) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        ) {
                            Text("+10m", fontSize = 11.sp, color = Colors.example5TextGreyLight)
                        }
                        TextButton(
                            onClick = { onSnooze(item.id, 30) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        ) {
                            Text("+30m", fontSize = 11.sp, color = Colors.example5TextGreyLight)
                        }
                    }
                }
            }
        }
    }
}
