package com.remindr.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.remindr.app.data.ai.PriorityEngine
import com.remindr.app.data.ai.PriorityItem
import com.remindr.app.data.model.Group
import com.remindr.app.data.model.Item
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.ui.components.FinancialSummary
import com.remindr.app.ui.components.GroupCard
import com.remindr.app.ui.components.PriorityFeed
import com.remindr.app.ui.theme.Colors
import com.remindr.app.util.getToday

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    items: List<Item>,
    groups: List<Group>,
    onItemClick: (Long) -> Unit,
    onStatusChange: (Long, ItemStatus) -> Unit,
    onGroupClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val today = remember { getToday() }
    val priorities = remember(items) {
        PriorityEngine.getTopPriorities(items, today)
    }

    Scaffold(
        containerColor = Colors.example5PageBgColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Remindr",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Colors.example5ToolbarColor,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            // Priority Feed
            if (priorities.isNotEmpty()) {
                item {
                    PriorityFeed(
                        priorities = priorities,
                        onItemClick = onItemClick,
                        onStatusChange = onStatusChange,
                    )
                }
            }

            // Financial Summary
            item {
                FinancialSummary(items = items, groups = groups)
            }

            // Groups Section
            if (groups.isNotEmpty()) {
                item {
                    Text(
                        text = "YOUR GROUPS",
                        style = MaterialTheme.typography.labelMedium,
                        color = Colors.example5TextGreyLight,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )
                }

                items(groups) { group ->
                    val groupItems = items.filter { it.groupId == group.id }
                    GroupCard(
                        group = group,
                        items = groupItems,
                        onClick = { onGroupClick(group.id) },
                    )
                }
            }

            // Ungrouped items section
            val ungroupedItems = items.filter { it.groupId == null && it.status != ItemStatus.COMPLETED && it.status != ItemStatus.ARCHIVED }
            if (ungroupedItems.isNotEmpty()) {
                item {
                    Text(
                        text = "UNGROUPED",
                        style = MaterialTheme.typography.labelMedium,
                        color = Colors.example5TextGreyLight,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )
                }
                item {
                    Text(
                        text = "${ungroupedItems.size} items without a group",
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
