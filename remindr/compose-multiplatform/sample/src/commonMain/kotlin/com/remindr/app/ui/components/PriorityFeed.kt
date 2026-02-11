package com.remindr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
    onItemClick: (Long) -> Unit,
    onStatusChange: (Long, ItemStatus) -> Unit,
) {
    if (priorities.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "UP NEXT",
            style = MaterialTheme.typography.labelMedium,
            color = Colors.example5TextGreyLight,
            modifier = Modifier.padding(start = 4.dp),
        )

        priorities.forEach { priority ->
            val item = priority.item
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
                            checkedColor = item.color,
                            uncheckedColor = Color.Gray,
                            checkmarkColor = Color.White,
                        ),
                    )

                    Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = priority.context,
                            color = when {
                                priority.context.contains("overdue") -> Color(0xFFDB504A)
                                priority.context.contains("today") -> Color(0xFFFF6F59)
                                priority.context.contains("tomorrow") -> Color(0xFFFCCA3E)
                                else -> Color.Gray
                            },
                            fontSize = 12.sp,
                        )
                    }

                    // Color indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(item.color, CircleShape),
                    )
                }
            }
        }
    }
}
