package com.remindr.app.ui.screens.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remindr.app.data.model.Group
import com.remindr.app.data.model.Item
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.ui.components.FinancialSummary
import com.remindr.app.ui.components.ItemRow
import com.remindr.app.ui.theme.Colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: Group,
    items: List<Item>,
    groups: List<Group>,
    onBack: () -> Unit,
    onItemClick: (Item) -> Unit,
    onItemDelete: (Item) -> Unit,
    onItemStatusChange: (Long, ItemStatus) -> Unit,
) {
    val parentItems = items.filter { it.parentId == null }
    val childItemsByParent = items.filter { it.parentId != null }.groupBy { it.parentId }

    Scaffold(
        containerColor = Colors.example5PageBgColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        group.name,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                .padding(innerPadding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            // Financial summary for this group
            val groupItems = items.filter { it.groupId == group.id }
            if (groupItems.any { it.amount != null }) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        FinancialSummary(items = groupItems, groups = groups)
                    }
                }
            }

            // Parent items
            items(parentItems) { item ->
                Box(
                    modifier = Modifier.clickable { onItemClick(item) },
                ) {
                    ItemRow(
                        item = item,
                        onDelete = { onItemDelete(item) },
                        onStatusChange = { status -> onItemStatusChange(item.id, status) },
                    )
                }

                // Sub-items (indented)
                val children = childItemsByParent[item.id] ?: emptyList()
                children.forEach { child ->
                    Box(
                        modifier = Modifier
                            .padding(start = 32.dp)
                            .clickable { onItemClick(child) },
                    ) {
                        ItemRow(
                            item = child,
                            onDelete = { onItemDelete(child) },
                            onStatusChange = { status -> onItemStatusChange(child.id, status) },
                        )
                    }
                }
            }

            // Bottom spacer
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
