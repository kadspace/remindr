package com.remindr.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kizitonwose.remindr.core.CalendarDay
import com.kizitonwose.remindr.core.DayPosition
import com.remindr.app.data.ai.AiService
import com.remindr.app.data.ai.ParsedItem
import com.remindr.app.data.db.DatabaseDriverFactory
import com.remindr.app.data.db.ItemRepository
import com.remindr.app.data.model.*
import com.remindr.app.db.RemindrDatabase
import com.remindr.app.domain.RecurrenceEngine
import com.remindr.app.domain.ReminderScheduler
import com.remindr.app.ui.components.InputBar
import com.remindr.app.ui.navigation.AppScreen
import com.remindr.app.ui.navigation.BottomTab
import com.remindr.app.ui.navigation.CalendarViewMode
import com.remindr.app.ui.screens.calendar.CalendarScreen
import com.remindr.app.ui.screens.group.GroupDetailScreen
import com.remindr.app.ui.screens.home.HomeScreen
import com.remindr.app.ui.screens.settings.SettingsScreen
import com.remindr.app.ui.theme.Colors
import com.remindr.app.util.getFormattedTime
import com.remindr.app.util.getToday
import com.remindr.app.util.getCurrentDateTime
import kotlinx.coroutines.launch
import kotlinx.datetime.*

private val pageBackgroundColor: Color = Colors.example5PageBgColor

@Composable
fun RemindrApp(
    driverFactory: DatabaseDriverFactory,
    requestMagicAdd: Boolean = false,
    scheduler: ReminderScheduler? = null,
    onRequestNotificationTest: ((String) -> Unit) -> Unit,
    onRequestRichNotificationTest: ((String) -> Unit) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    // Database Init
    val database = remember { RemindrDatabase(driverFactory.createDriver()) }
    val repository = remember { ItemRepository(database) }

    // Navigation state
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    var currentTab by remember { mutableStateOf(BottomTab.Home) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.Month) }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }

    // Data
    val items = repository.getAllItems().collectAsState(initial = emptyList()).value
    val groups = repository.getAllGroups().collectAsState(initial = emptyList()).value

    // Calendar state
    var selection by remember { mutableStateOf<CalendarDay?>(null) }
    var recentlyAddedDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(recentlyAddedDate) {
        if (recentlyAddedDate != null) {
            kotlinx.coroutines.delay(2000)
            recentlyAddedDate = null
        }
    }

    // Settings
    var apiKey by remember { mutableStateOf("") }
    var eventTypeLabels by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        repository.getApiKey()?.let { apiKey = it }
        eventTypeLabels = repository.getEventTypeLabels()
    }

    // Input bar state
    var isSaving by remember { mutableStateOf(false) }

    // Debug logs
    var debugLogs by remember { mutableStateOf("Logs will appear here...\n") }

    // AI Service
    val aiService = remember { AiService() }

    // BackHandler
    com.remindr.app.ui.components.BackHandler(
        enabled = currentScreen != AppScreen.Home,
    ) {
        when {
            currentScreen == AppScreen.Settings -> currentScreen = AppScreen.Home
            currentScreen == AppScreen.GroupDetail -> currentScreen = AppScreen.Home
            selection != null -> selection = null
        }
    }

    fun handleAiInput(inputText: String) {
        coroutineScope.launch {
            isSaving = true
            try {
                debugLogs += "${getFormattedTime()}: Requesting AI for: '$inputText'\n"
                val today = getToday()
                val now = getCurrentDateTime()
                var parsedItem: ParsedItem? = null

                if (apiKey.isNotBlank()) {
                    try {
                        val groupNames = groups.map { it.name }
                        parsedItem = aiService.parseItem(inputText, apiKey, today, groupNames)
                        debugLogs += "${getFormattedTime()}: AI Response: $parsedItem\n"
                    } catch (e: Exception) {
                        debugLogs += "${getFormattedTime()}: AI Error: ${e.message}\n"
                    }
                }

                val title = parsedItem?.title ?: "Reminder"
                val desc = parsedItem?.description ?: inputText
                val noteDate = if (parsedItem?.year != null) LocalDate(parsedItem.year, parsedItem.month!!, parsedItem.day!!) else today
                val noteTime = if (parsedItem != null) LocalTime(parsedItem.hour, parsedItem.minute) else LocalTime(12, 0)

                // Handle group assignment
                var groupId: Long? = null
                if (parsedItem?.groupName != null) {
                    if (parsedItem.isNewGroup) {
                        val newGroup = Group(
                            name = parsedItem.groupName,
                            createdAt = now,
                            updatedAt = now,
                        )
                        repository.insertGroup(newGroup)
                        groupId = repository.getLastInsertedGroupId()
                    } else {
                        groupId = repository.getGroupByName(parsedItem.groupName)?.id
                    }
                }

                val itemType = try {
                    ItemType.valueOf(parsedItem?.type ?: "TASK")
                } catch (_: Exception) { ItemType.TASK }

                val newItem = Item(
                    id = -1L,
                    title = title,
                    description = desc,
                    time = noteDate.atTime(noteTime),
                    color = if (parsedItem != null) Colors.noteColors.getOrElse(parsedItem.colorIndex) { Colors.accent } else Colors.accent,
                    severity = if (parsedItem?.severity != null) try { Severity.valueOf(parsedItem.severity) } catch (_: Exception) { Severity.MEDIUM } else Severity.MEDIUM,
                    type = itemType,
                    status = ItemStatus.PENDING,
                    groupId = groupId,
                    amount = parsedItem?.amount,
                    recurrenceType = parsedItem?.recurrenceType,
                    recurrenceEndDate = if (parsedItem?.recurrenceEndYear != null && parsedItem.recurrenceEndMonth != null) {
                        LocalDate(parsedItem.recurrenceEndYear, parsedItem.recurrenceEndMonth, 1).atTime(0, 0)
                    } else null,
                    nagEnabled = parsedItem?.nagEnabled ?: false,
                    reminderOffsets = parsedItem?.reminderOffsets ?: listOf(0L),
                    createdAt = now,
                )

                repository.insert(newItem)

                // Schedule reminder
                val newId = repository.getLastInsertedItemId()
                if (newId != null && newItem.reminderOffsets.isNotEmpty()) {
                    scheduler?.schedule(newItem.copy(id = newId))
                }

                selection = CalendarDay(noteDate, DayPosition.MonthDate)
                recentlyAddedDate = noteDate
            } finally {
                isSaving = false
            }
        }
    }

    fun handleStatusChange(itemId: Long, status: ItemStatus) {
        repository.updateStatus(itemId, status)

        if (status == ItemStatus.COMPLETED) {
            val item = repository.getItemById(itemId)
            if (item != null) {
                scheduler?.cancel(item)

                // Handle recurrence
                if (item.recurrenceType != null) {
                    val nextDate = RecurrenceEngine.getNextDueDate(item)
                    if (nextDate != null) {
                        val now = getCurrentDateTime()
                        val newItem = item.copy(
                            id = -1,
                            time = nextDate,
                            status = ItemStatus.PENDING,
                            lastCompletedAt = null,
                            createdAt = now,
                        )
                        repository.insert(newItem)
                        val newId = repository.getLastInsertedItemId()
                        if (newId != null) {
                            scheduler?.schedule(newItem.copy(id = newId))
                        }
                    }
                }
            }
        }
    }

    // Main Layout
    when (currentScreen) {
        AppScreen.Settings -> {
            SettingsScreen(
                apiKey = apiKey,
                onApiKeyChange = { newKey ->
                    apiKey = newKey
                    repository.saveApiKey(newKey)
                },
                onTestNotification = {
                    onRequestNotificationTest { message ->
                        debugLogs += "${getFormattedTime()}: $message\n"
                    }
                },
                onRichTestNotification = {
                    onRequestRichNotificationTest { message ->
                        debugLogs += "${getFormattedTime()}: $message\n"
                    }
                },
                logs = debugLogs,
                eventTypeLabels = eventTypeLabels,
                onEventTypeLabelsChange = { newLabels ->
                    eventTypeLabels = newLabels
                    repository.saveEventTypeLabels(newLabels)
                },
                onBack = { currentScreen = AppScreen.Home },
            )
        }

        AppScreen.GroupDetail -> {
            val group = selectedGroupId?.let { repository.getGroupById(it) }
            if (group != null) {
                val groupItems = items.filter { it.groupId == group.id }
                GroupDetailScreen(
                    group = group,
                    items = groupItems,
                    groups = groups,
                    onBack = { currentScreen = AppScreen.Home },
                    onItemClick = { },
                    onItemDelete = { item ->
                        repository.deleteById(item.id)
                        scheduler?.cancel(item)
                    },
                    onItemStatusChange = { id, status -> handleStatusChange(id, status) },
                )
            }
        }

        AppScreen.Home, AppScreen.Calendar -> {
            Scaffold(
                containerColor = pageBackgroundColor,
                bottomBar = {
                    Column {
                        // Input bar sits right above the nav bar
                        InputBar(
                            placeholder = when (currentTab) {
                                BottomTab.Home -> "New Reminder..."
                                BottomTab.Calendar -> if (selection != null) "Add to ${selection?.date}..." else "New Reminder..."
                            },
                            isSaving = isSaving,
                            onSend = { text -> handleAiInput(text) },
                        )
                        NavigationBar(
                            containerColor = Colors.example5ToolbarColor,
                            contentColor = Color.White,
                            modifier = Modifier.navigationBarsPadding(),
                        ) {
                            NavigationBarItem(
                                selected = currentTab == BottomTab.Home,
                                onClick = {
                                    currentTab = BottomTab.Home
                                    currentScreen = AppScreen.Home
                                },
                                icon = { Icon(Icons.Default.Home, "Home") },
                                label = { Text("Home") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Colors.accent,
                                    selectedTextColor = Colors.accent,
                                    indicatorColor = Colors.accent.copy(alpha = 0.15f),
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray,
                                ),
                            )
                            NavigationBarItem(
                                selected = currentTab == BottomTab.Calendar,
                                onClick = {
                                    currentTab = BottomTab.Calendar
                                    currentScreen = AppScreen.Calendar
                                },
                                icon = { Icon(Icons.Default.DateRange, "Calendar") },
                                label = { Text("Calendar") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Colors.accent,
                                    selectedTextColor = Colors.accent,
                                    indicatorColor = Colors.accent.copy(alpha = 0.15f),
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray,
                                ),
                            )
                        }
                    }
                },
            ) { scaffoldPadding ->
                Box(modifier = Modifier.padding(scaffoldPadding)) {
                    when (currentTab) {
                        BottomTab.Home -> {
                            HomeScreen(
                                items = items,
                                groups = groups,
                                onItemClick = { id -> handleStatusChange(id, ItemStatus.COMPLETED) },
                                onStatusChange = { id, status -> handleStatusChange(id, status) },
                                onGroupClick = { groupId ->
                                    selectedGroupId = groupId
                                    currentScreen = AppScreen.GroupDetail
                                },
                                onSettingsClick = { currentScreen = AppScreen.Settings },
                            )
                        }
                        BottomTab.Calendar -> {
                            CalendarScreen(
                                items = items,
                                groups = groups,
                                selection = selection,
                                onSelectionChange = { selection = it },
                                recentlyAddedDate = recentlyAddedDate,
                                onItemClick = { },
                                onItemDelete = { item ->
                                    repository.deleteById(item.id)
                                    scheduler?.cancel(item)
                                },
                                onItemStatusChange = { id, status -> handleStatusChange(id, status) },
                                onSettingsClick = { currentScreen = AppScreen.Settings },
                                viewMode = viewMode,
                                onViewModeToggle = {
                                    viewMode = if (viewMode == CalendarViewMode.Month) CalendarViewMode.Year else CalendarViewMode.Month
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
