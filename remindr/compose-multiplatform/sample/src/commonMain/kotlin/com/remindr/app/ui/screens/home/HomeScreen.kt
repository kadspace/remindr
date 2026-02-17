package com.remindr.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remindr.app.data.ai.PriorityEngine
import com.remindr.app.data.model.Item
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.ui.components.PriorityFeed
import com.remindr.app.ui.theme.Colors
import com.remindr.app.util.getToday

private enum class HomeFilter { ACTIVE, COMPLETED }

@Composable
fun HomeScreen(
    items: List<Item>,
    onStatusChange: (Long, ItemStatus) -> Unit,
    onSnooze: (Long, Int) -> Unit,
    onItemClick: (Long) -> Unit,
) {
    val today = remember { getToday() }
    var filter by remember { mutableStateOf(HomeFilter.ACTIVE) }

    val activeItems = remember(items) {
        items
            .filter { it.status != ItemStatus.COMPLETED && it.status != ItemStatus.ARCHIVED }
            .sortedBy { it.time ?: it.createdAt }
    }
    val completedItems = remember(items) {
        items
            .filter { it.status == ItemStatus.COMPLETED }
            .sortedByDescending { it.lastCompletedAt ?: it.createdAt }
    }
    val archivedItems = remember(items) {
        items
            .filter { it.status == ItemStatus.ARCHIVED }
            .sortedByDescending { it.createdAt }
    }

    val ranked = remember(activeItems) {
        PriorityEngine.rankAll(activeItems, today)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val tabs = listOf(HomeFilter.ACTIVE, HomeFilter.COMPLETED)
        TabRow(selectedTabIndex = tabs.indexOf(filter)) {
            tabs.forEach { tab ->
                Tab(
                    selected = filter == tab,
                    onClick = { filter = tab },
                    text = {
                        Text(
                            if (tab == HomeFilter.ACTIVE) "Active" else "Completed",
                        )
                    },
                )
            }
        }

        when (filter) {
            HomeFilter.ACTIVE -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    item {
                        if (ranked.isEmpty()) {
                            Text(
                                text = "No active reminders.",
                                color = Colors.example5TextGreyLight,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        } else {
                            PriorityFeed(
                                priorities = ranked,
                                onStatusChange = onStatusChange,
                                onSnooze = onSnooze,
                                onItemClick = onItemClick,
                            )
                        }
                    }
                }
            }

            HomeFilter.COMPLETED -> {
                CompletedWithArchiveList(
                    completedItems = completedItems,
                    archivedItems = archivedItems,
                    onStatusChange = onStatusChange,
                )
            }
        }
    }
}

@Composable
private fun CompletedWithArchiveList(
    completedItems: List<Item>,
    archivedItems: List<Item>,
    onStatusChange: (Long, ItemStatus) -> Unit,
) {
    var showArchived by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        if (completedItems.isEmpty()) {
            item {
                Text(
                    text = "No completed reminders yet.",
                    color = Colors.example5TextGreyLight,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            items(items = completedItems, key = { it.id }) { item ->
                ReminderHistoryCard(
                    item = item,
                    primaryActionLabel = if (item.recurrenceType == null) "Mark Active" else "Keep Completed",
                    primaryActionEnabled = item.recurrenceType == null,
                    onPrimaryAction = { onStatusChange(item.id, ItemStatus.PENDING) },
                    secondaryActionLabel = "Archive",
                    onSecondaryAction = { onStatusChange(item.id, ItemStatus.ARCHIVED) },
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showArchived = !showArchived }
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Archived (${archivedItems.size})",
                    color = Colors.example5TextGreyLight,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (showArchived) "Hide" else "Show",
                    color = Colors.example5TextGreyLight,
                )
            }
        }

        if (showArchived) {
            if (archivedItems.isEmpty()) {
                item {
                    Text(
                        text = "No archived reminders.",
                        color = Colors.example5TextGreyLight,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            } else {
                items(items = archivedItems, key = { it.id }) { item ->
                    ReminderHistoryCard(
                        item = item,
                        primaryActionLabel = "Restore",
                        onPrimaryAction = { onStatusChange(item.id, ItemStatus.PENDING) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderHistoryCard(
    item: Item,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    primaryActionEnabled: Boolean = true,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    val isDoneOrArchived = item.status == ItemStatus.COMPLETED || item.status == ItemStatus.ARCHIVED
    val titleColor = if (isDoneOrArchived) Colors.reminderDoneGray else Colors.reminderActiveRed

    Card(
        colors = CardDefaults.cardColors(containerColor = Colors.example5ItemViewBgColor),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item.title,
                color = titleColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Text(
                text = item.dueSummary,
                color = Colors.example5TextGreyLight,
                fontSize = 12.sp,
            )
            item.recurrenceSummary?.let { recurrence ->
                Text(
                    text = recurrence,
                    color = Colors.example5TextGreyLight,
                    fontSize = 12.sp,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onPrimaryAction,
                    enabled = primaryActionEnabled,
                ) {
                    Text(primaryActionLabel)
                }
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    OutlinedButton(onClick = onSecondaryAction) {
                        Text(secondaryActionLabel)
                    }
                }
            }
        }
    }
}
