package com.remindr.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun FinancialSummary(
    items: List<Item>,
    groups: List<Group>,
) {
    val financialItems = items.filter {
        it.amount != null && it.status != ItemStatus.ARCHIVED
    }
    if (financialItems.isEmpty()) return

    val monthlyTotal = calculateMonthlyTotal(financialItems)
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = itemBackgroundColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "MONTHLY OBLIGATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Colors.example5TextGreyLight,
                    )
                    Text(
                        text = "$${String.format("%,.0f", monthlyTotal)}",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    tint = Color.Gray,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Per-group breakdown
                    val groupedItems = financialItems.groupBy { it.groupId }

                    groupedItems.forEach { (groupId, groupItems) ->
                        val groupName = if (groupId != null) {
                            groups.find { it.id == groupId }?.name ?: "Other"
                        } else {
                            "Ungrouped"
                        }
                        val groupTotal = calculateMonthlyTotal(groupItems)
                        val ratio = if (monthlyTotal > 0) (groupTotal / monthlyTotal).toFloat() else 0f

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text = groupName, color = Color.White, fontSize = 13.sp)
                                Text(
                                    text = "$${String.format("%,.0f", groupTotal)}/mo",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .padding(top = 4.dp),
                                color = Colors.accent,
                                trackColor = Color.Gray.copy(alpha = 0.2f),
                            )
                        }
                    }
                }
            }
        }
    }
}

fun calculateMonthlyTotal(items: List<Item>): Double {
    return items
        .filter { it.amount != null && it.status != ItemStatus.ARCHIVED }
        .sumOf { item ->
            when (item.recurrenceType) {
                "DAILY" -> (item.amount ?: 0.0) * 30
                "WEEKLY" -> (item.amount ?: 0.0) * 4.33
                "MONTHLY" -> item.amount ?: 0.0
                "YEARLY" -> (item.amount ?: 0.0) / 12
                null -> 0.0
                else -> 0.0
            }
        }
}
