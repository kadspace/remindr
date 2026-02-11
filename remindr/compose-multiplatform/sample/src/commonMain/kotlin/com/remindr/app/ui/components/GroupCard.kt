package com.remindr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remindr.app.data.model.Group
import com.remindr.app.data.model.Item
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.ui.theme.Colors

private val itemBackgroundColor: Color = Colors.example5ItemViewBgColor

@Composable
fun GroupCard(
    group: Group,
    items: List<Item>,
    onClick: () -> Unit,
) {
    val totalItems = items.size
    val completedItems = items.count { it.status == ItemStatus.COMPLETED }
    val progress = if (totalItems > 0) completedItems.toFloat() / totalItems else 0f
    val monthlyAmount = items.filter { it.amount != null && it.recurrenceType != null }
        .sumOf { item ->
            when (item.recurrenceType) {
                "DAILY" -> (item.amount ?: 0.0) * 30
                "WEEKLY" -> (item.amount ?: 0.0) * 4.33
                "MONTHLY" -> item.amount ?: 0.0
                "YEARLY" -> (item.amount ?: 0.0) / 12
                else -> 0.0
            }
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = itemBackgroundColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Colors.accent.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = group.icon ?: group.name.take(1).uppercase(),
                        fontSize = 18.sp,
                        color = Colors.accent,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    val summary = buildString {
                        append("$completedItems of $totalItems complete")
                        if (monthlyAmount > 0) {
                            append(" · $${monthlyAmount.toInt()}/mo")
                        }
                    }
                    Text(
                        text = summary,
                        color = Color.Gray,
                        fontSize = 12.sp,
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open",
                    tint = Color.Gray,
                )
            }

            if (totalItems > 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Colors.accent,
                    trackColor = Color.Gray.copy(alpha = 0.2f),
                )
            }
        }
    }
}
