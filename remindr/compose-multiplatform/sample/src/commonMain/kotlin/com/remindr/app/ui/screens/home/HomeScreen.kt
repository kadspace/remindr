package com.remindr.app.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remindr.app.data.model.Item
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.ui.components.ReminderRow
import com.remindr.app.ui.theme.Colors
import kotlinx.datetime.LocalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    items: List<Item>,
    onStatusChange: (Long, ItemStatus) -> Unit,
    onItemClick: (Long) -> Unit,
) {
    var showCompleted by rememberSaveable { mutableStateOf(false) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var showDeleted by rememberSaveable { mutableStateOf(false) }
    var hiddenRows by remember { mutableStateOf(setOf<Pair<Long, ItemStatus>>()) }
    val coroutineScope = rememberCoroutineScope()

    val currentRows = remember(items) { items.map { it.id to it.status }.toSet() }
    LaunchedEffect(currentRows) {
        hiddenRows = hiddenRows.filterTo(mutableSetOf()) { it in currentRows }
    }

    fun hideOptimistically(item: Item) {
        val key = item.id to item.status
        hiddenRows = hiddenRows + key
        coroutineScope.launch {
            delay(1500)
            hiddenRows = hiddenRows - key
        }
    }

    val activeItems = remember(items, hiddenRows) {
        items
            .filter { it.status !in setOf(ItemStatus.COMPLETED, ItemStatus.ARCHIVED, ItemStatus.DELETED) }
            .filter { (it.id to it.status) !in hiddenRows }
            .let(::sortActiveRemindersForQueue)
    }
    val completedItems = remember(items, hiddenRows) {
        items
            .filter { it.status == ItemStatus.COMPLETED }
            .filter { (it.id to it.status) !in hiddenRows }
            .sortedByDescending { it.lastCompletedAt ?: it.createdAt }
    }
    val archivedItems = remember(items, hiddenRows) {
        items
            .filter { it.status == ItemStatus.ARCHIVED }
            .filter { (it.id to it.status) !in hiddenRows }
            .sortedByDescending { it.createdAt }
    }
    val deletedItems = remember(items, hiddenRows) {
        items
            .filter { it.status == ItemStatus.DELETED }
            .filter { (it.id to it.status) !in hiddenRows }
            .sortedByDescending { it.createdAt }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp),
    ) {
        if (activeItems.isEmpty()) {
            item {
                Text(
                    text = "No active reminders.",
                    color = Colors.example5TextGreyLight,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                )
            }
        } else {
            items(activeItems, key = { it.id }) { item ->
                SwipeableReminderRow(
                    item = item,
                    primaryActionLabel = "Archive",
                    secondaryActionLabel = "Delete",
                    onPrimaryAction = {
                        hideOptimistically(item)
                        onStatusChange(item.id, ItemStatus.ARCHIVED)
                    },
                    onSecondaryAction = {
                        hideOptimistically(item)
                        onStatusChange(item.id, ItemStatus.DELETED)
                    },
                    onStatusChange = { status -> onStatusChange(item.id, status) },
                    onOpen = { onItemClick(item.id) },
                )
            }
        }

        item {
            SectionToggle(
                label = "Completed",
                count = completedItems.size,
                expanded = showCompleted,
                onToggle = { showCompleted = !showCompleted },
            )
        }

        if (showCompleted) {
            if (completedItems.isEmpty()) {
                item {
                    Text(
                        text = "No completed reminders.",
                        color = Colors.example5TextGreyLight,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 6.dp, top = 2.dp),
                    )
                }
            } else {
                items(completedItems, key = { it.id }) { item ->
                    SwipeableReminderRow(
                        item = item,
                        primaryActionLabel = "Restore",
                        secondaryActionLabel = "Delete",
                        onPrimaryAction = {
                            hideOptimistically(item)
                            onStatusChange(item.id, ItemStatus.PENDING)
                        },
                        onSecondaryAction = {
                            hideOptimistically(item)
                            onStatusChange(item.id, ItemStatus.DELETED)
                        },
                        onStatusChange = { status -> onStatusChange(item.id, status) },
                        onOpen = { onItemClick(item.id) },
                    )
                }
            }
        }

        item {
            SectionToggle(
                label = "Archived",
                count = archivedItems.size,
                expanded = showArchived,
                onToggle = { showArchived = !showArchived },
            )
        }

        if (showArchived) {
            if (archivedItems.isEmpty()) {
                item {
                    Text(
                        text = "No archived reminders.",
                        color = Colors.example5TextGreyLight,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 6.dp, top = 2.dp),
                    )
                }
            } else {
                items(archivedItems, key = { it.id }) { item ->
                    SwipeableReminderRow(
                        item = item,
                        primaryActionLabel = "Restore",
                        secondaryActionLabel = "Delete",
                        onPrimaryAction = {
                            hideOptimistically(item)
                            onStatusChange(item.id, ItemStatus.PENDING)
                        },
                        onSecondaryAction = {
                            hideOptimistically(item)
                            onStatusChange(item.id, ItemStatus.DELETED)
                        },
                        onStatusChange = { status -> onStatusChange(item.id, status) },
                        onOpen = { onItemClick(item.id) },
                    )
                }
            }
        }

        item {
            SectionToggle(
                label = "Deleted",
                count = deletedItems.size,
                expanded = showDeleted,
                onToggle = { showDeleted = !showDeleted },
            )
        }

        if (showDeleted) {
            if (deletedItems.isEmpty()) {
                item {
                    Text(
                        text = "No deleted reminders.",
                        color = Colors.example5TextGreyLight,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 6.dp, top = 2.dp),
                    )
                }
            } else {
                items(deletedItems, key = { it.id }) { item ->
                    SwipeableReminderRow(
                        item = item,
                        primaryActionLabel = "Restore",
                        secondaryActionLabel = null,
                        onPrimaryAction = {
                            hideOptimistically(item)
                            onStatusChange(item.id, ItemStatus.PENDING)
                        },
                        onSecondaryAction = null,
                        onStatusChange = { status -> onStatusChange(item.id, status) },
                        onOpen = { onItemClick(item.id) },
                    )
                }
            }
        }
    }
}

private fun sortActiveRemindersForQueue(items: List<Item>): List<Item> {
    val fallbackTime = LocalTime(23, 59)
    return items.sortedWith(
        compareBy<Item>(
            { it.time == null },
            { it.time?.date?.toEpochDays() ?: Int.MAX_VALUE },
            { it.time?.time ?: fallbackTime },
            { it.createdAt },
            { it.id },
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableReminderRow(
    item: Item,
    primaryActionLabel: String,
    secondaryActionLabel: String?,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: (() -> Unit)?,
    onStatusChange: (ItemStatus) -> Unit,
    onOpen: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { total -> total * 0.45f },
    )
    var actionHandled by remember(item.id, item.status) { mutableStateOf(false) }

    LaunchedEffect(dismissState.currentValue, onSecondaryAction) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                if (!actionHandled) {
                    actionHandled = true
                    delay(110)
                    onPrimaryAction()
                }
            }

            SwipeToDismissBoxValue.EndToStart -> {
                if (!actionHandled && onSecondaryAction != null) {
                    actionHandled = true
                    delay(110)
                    onSecondaryAction()
                }
            }

            SwipeToDismissBoxValue.Settled -> actionHandled = false
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = onSecondaryAction != null,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isArmed = dismissState.targetValue != SwipeToDismissBoxValue.Settled
            val label = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> primaryActionLabel
                SwipeToDismissBoxValue.EndToStart -> secondaryActionLabel.orEmpty()
                SwipeToDismissBoxValue.Settled -> ""
            }
            val backgroundColor = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> if (isArmed) Color(0xFF2A5A3C) else Color(0xFF1F3529)
                SwipeToDismissBoxValue.EndToStart -> if (isArmed) Color(0xFF6A2A2A) else Color(0xFF4A1F1F)
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.Settled -> Alignment.Center
            }
            val labelAlpha by animateFloatAsState(
                targetValue = if (direction == SwipeToDismissBoxValue.Settled) 0f else if (isArmed) 1f else 0.82f,
                animationSpec = tween(durationMillis = 140),
                label = "reminderSwipeLabelAlpha",
            )
            val labelScale by animateFloatAsState(
                targetValue = if (isArmed) 1.07f else 0.94f,
                animationSpec = tween(durationMillis = 140),
                label = "reminderSwipeLabelScale",
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = alignment,
            ) {
                if (label.isNotBlank()) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.graphicsLayer {
                            alpha = labelAlpha
                            scaleX = labelScale
                            scaleY = labelScale
                        },
                    )
                }
            }
        },
    ) {
        ReminderRow(
            item = item,
            onStatusChange = onStatusChange,
            onEdit = onOpen,
        )
    }
}

@Composable
private fun SectionToggle(
    label: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) Color.White.copy(alpha = 0.12f) else Colors.example5ToolbarColor.copy(alpha = 0.8f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = count.toString(),
                color = Colors.example5TextGreyLight,
                fontSize = 11.sp,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse $label" else "Expand $label",
                tint = Colors.example5TextGreyLight,
            )
        }
    }
}
