package com.remindr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
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
import com.remindr.app.data.model.ItemType
import com.remindr.app.ui.theme.Colors

private val pageBackgroundColor: Color = Colors.example5PageBgColor
private val itemBackgroundColor: Color = Colors.example5ItemViewBgColor

@Composable
fun LazyItemScope.ItemRow(
    item: Item,
    onDelete: () -> Unit,
    onStatusChange: (ItemStatus) -> Unit,
    showGroupBadge: Boolean = false,
    groupName: String? = null,
) {
    val isCompleted = item.isCompleted

    Row(
        modifier = Modifier
            .fillParentMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Checkbox
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
                    checkedColor = item.color,
                    uncheckedColor = Color.Gray,
                    checkmarkColor = Color.White,
                ),
            )
        }

        // Color + time block
        Box(
            modifier = Modifier
                .background(color = item.color)
                .fillParentMaxWidth(1 / 7f)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (item.time != null) {
                    Text(
                        text = "${item.time.hour}:${item.time.minute.toString().padStart(2, '0')}",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 12.sp,
                    )
                }
                if (item.amount != null) {
                    Text(
                        text = "$${item.amount.toInt()}",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }

        // Content
        Box(
            modifier = Modifier
                .background(color = if (isCompleted) itemBackgroundColor.copy(alpha = 0.5f) else itemBackgroundColor)
                .weight(1f)
                .fillMaxHeight()
                .padding(8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.text,
                        color = if (isCompleted) Color.Gray else Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    // Type chip
                    if (item.type != ItemType.TASK) {
                        Text(
                            text = item.type.name,
                            fontSize = 8.sp,
                            color = Color.White,
                            modifier = Modifier
                                .background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
                if (showGroupBadge && groupName != null) {
                    Text(
                        text = groupName,
                        fontSize = 10.sp,
                        color = Color.Gray,
                    )
                }
                // Status chip if not PENDING
                if (item.status != ItemStatus.PENDING && item.status != ItemStatus.COMPLETED) {
                    Text(
                        text = item.status.name.replace("_", " "),
                        fontSize = 9.sp,
                        color = when (item.status) {
                            ItemStatus.IN_PROGRESS -> Color(0xFF43AA8B)
                            ItemStatus.MONITORING -> Color(0xFFFCCA3E)
                            else -> Color.Gray
                        },
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .background(
                                when (item.status) {
                                    ItemStatus.IN_PROGRESS -> Color(0xFF43AA8B).copy(alpha = 0.15f)
                                    ItemStatus.MONITORING -> Color(0xFFFCCA3E).copy(alpha = 0.15f)
                                    else -> Color.Gray.copy(alpha = 0.15f)
                                },
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
        }

        // Delete button
        Box(
            modifier = Modifier
                .background(color = itemBackgroundColor)
                .fillMaxHeight()
                .clickable(onClick = onDelete)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete",
                tint = Color.Gray,
            )
        }
    }
    HorizontalDivider(thickness = 2.dp, color = pageBackgroundColor)
}
