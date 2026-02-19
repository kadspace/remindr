package com.remindr.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.remindr.core.CalendarDay
import com.kizitonwose.remindr.core.DayPosition
import com.remindr.app.data.ai.AiService
import com.remindr.app.data.ai.ParsedItem
import com.remindr.app.data.db.DatabaseDriverFactory
import com.remindr.app.data.db.ItemRepository
import com.remindr.app.data.db.QuickNoteRepository
import com.remindr.app.data.model.*
import com.remindr.app.db.RemindrDatabase
import com.remindr.app.domain.ReminderScheduler
import com.remindr.app.ui.components.EditSheet
import com.remindr.app.ui.components.InputBar
import com.remindr.app.ui.components.RemindrWordmark
import com.remindr.app.ui.navigation.AppScreen
import com.remindr.app.ui.navigation.BottomTab
import com.remindr.app.ui.navigation.CalendarViewMode
import com.remindr.app.ui.screens.calendar.CalendarScreen
import com.remindr.app.ui.screens.home.HomeScreen
import com.remindr.app.ui.screens.notes.NoteEditorScreen
import com.remindr.app.ui.screens.notes.NotesScreen
import com.remindr.app.ui.screens.settings.SettingsScreen
import com.remindr.app.ui.theme.Colors
import com.remindr.app.util.getFormattedTime
import com.remindr.app.util.getToday
import com.remindr.app.util.getCurrentDateTime
import com.remindr.app.util.formatTime12
import com.remindr.app.util.parseUsOrIsoLocalDateOrNull
import com.remindr.app.util.toUsDateString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.*

private val pageBackgroundColor: Color = Colors.example5PageBgColor
private const val mockSeedKey = "__mock_seed_version__"
private const val mockSeedVersion = "2026-02-19"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindrApp(
    driverFactory: DatabaseDriverFactory,
    requestMagicAdd: Boolean = false,
    scheduler: ReminderScheduler? = null,
    appVersionLabel: String = "dev",
) {
    val coroutineScope = rememberCoroutineScope()

    // Database Init
    val database = remember { RemindrDatabase(driverFactory.createDriver()) }
    val repository = remember { ItemRepository(database) }
    val quickNoteRepository = remember { QuickNoteRepository(database) }

    // Navigation state
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    var currentTab by remember { mutableStateOf(BottomTab.Home) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.Month) }

    // Data
    val items = repository.getAllItems().collectAsState(initial = emptyList()).value
    val notes = quickNoteRepository.getAllNotes().collectAsState(initial = emptyList()).value

    // Calendar state
    var selection by remember { mutableStateOf<CalendarDay?>(null) }
    var recentlyAddedDate by remember { mutableStateOf<LocalDate?>(null) }
    var editingItemId by remember { mutableStateOf<Long?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editRecurrenceType by remember { mutableStateOf<String?>(null) }
    var editIntervalText by remember { mutableStateOf("1") }
    var editDueDateText by remember { mutableStateOf("") }
    var editDueTimeText by remember { mutableStateOf("") }
    var editEndMode by remember { mutableStateOf("NEVER") }
    var editEndDateText by remember { mutableStateOf("") }
    var editingNoteId by remember { mutableStateOf<Long?>(null) }
    var noteEditorText by remember { mutableStateOf("") }
    var noteEditorInitialText by remember { mutableStateOf("") }

    LaunchedEffect(recentlyAddedDate) {
        if (recentlyAddedDate != null) {
            kotlinx.coroutines.delay(2000)
            recentlyAddedDate = null
        }
    }

    // Settings
    var apiKey by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val seeded = repository.getSetting(mockSeedKey)
        if (seeded != mockSeedVersion) {
            repository.clearAllData()
            database.groupTableQueries.deleteAll()
            quickNoteRepository.clearAll()
            seedMockData(repository, quickNoteRepository)
            repository.saveSetting(mockSeedKey, mockSeedVersion)
        }
        apiKey = repository.getApiKey().orEmpty()
    }

    // Input bar state
    var isSaving by remember { mutableStateOf(false) }
    var isComposerOpen by remember { mutableStateOf(false) }
    var composerFocusTick by remember { mutableStateOf(0) }

    // Debug logs
    var debugLogs by remember { mutableStateOf("Logs will appear here...\n") }

    // AI Service
    val aiService = remember { AiService() }

    suspend fun createReminderFromInput(inputText: String): Long? {
        debugLogs += "${getFormattedTime()}: Requesting AI for: '$inputText'\n"
        val today = getToday()
        val now = getCurrentDateTime()
        var parsedItem: ParsedItem? = null

        if (apiKey.isNotBlank()) {
            try {
                parsedItem = aiService.parseItem(inputText, apiKey, today)
                debugLogs += "${getFormattedTime()}: AI Response: $parsedItem\n"
            } catch (e: Exception) {
                debugLogs += "${getFormattedTime()}: AI Error: ${e.message}\n"
            }
        }

        val title = resolveTitleForInput(parsedItem, inputText)
        val explicitDate = extractExplicitDateFromInput(inputText, today)
        val recurrenceType = resolveRecurrenceTypeForInput(
            parsedItem = parsedItem,
            rawInput = inputText,
            explicitDate = explicitDate,
        )
        val noteDate = resolveDateForInput(
            parsedItem = parsedItem,
            rawInput = inputText,
            today = today,
            recurrenceType = recurrenceType,
            explicitDate = explicitDate,
        )
        val noteTime = parsedItem?.safeTimeOrDefault() ?: LocalTime(12, 0)

        val newItem = Item(
            id = -1L,
            title = title,
            description = null,
            time = noteDate.atTime(noteTime),
            color = Colors.example5TextGrey,
            severity = Severity.MEDIUM,
            type = ItemType.TASK,
            status = ItemStatus.PENDING,
            groupId = null,
            amount = parsedItem?.amount,
            recurrenceType = recurrenceType,
            recurrenceInterval = 1,
            recurrenceEndMode = if (recurrenceType == null) "NEVER" else if (parsedItem.safeRecurrenceEndDateOrNull() != null) "UNTIL_DATE" else "NEVER",
            recurrenceEndDate = parsedItem.safeRecurrenceEndDateOrNull(),
            nagEnabled = parsedItem?.nagEnabled ?: false,
            reminderOffsets = parsedItem?.reminderOffsets?.takeIf { it.isNotEmpty() } ?: listOf(0L),
            createdAt = now,
        )

        repository.insert(newItem)
        val newId = repository.getLastInsertedItemId()

        if (newId != null && newItem.reminderOffsets.isNotEmpty()) {
            scheduler?.schedule(newItem.copy(id = newId))
        }

        selection = CalendarDay(noteDate, DayPosition.MonthDate)
        recentlyAddedDate = noteDate
        return newId
    }

    fun handleAiInput(inputText: String) {
        coroutineScope.launch {
            isSaving = true
            try {
                createReminderFromInput(inputText)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                debugLogs += "${getFormattedTime()}: Save Error: ${e.message}\n"
            } finally {
                isSaving = false
            }
        }
    }

    fun handleQuickNoteInput(inputText: String) {
        val noteId = quickNoteRepository.insert(inputText) ?: return
        debugLogs += "${getFormattedTime()}: Saved quick note $noteId\n"
    }

    fun openNewNoteEditor() {
        isComposerOpen = false
        currentTab = BottomTab.Notes
        currentScreen = AppScreen.NoteEditor
        editingNoteId = null
        noteEditorText = ""
        noteEditorInitialText = ""
    }

    fun openExistingNoteEditor(note: QuickNote) {
        isComposerOpen = false
        currentTab = BottomTab.Notes
        currentScreen = AppScreen.NoteEditor
        editingNoteId = note.id
        noteEditorText = note.content
        noteEditorInitialText = note.content
    }

    fun persistNoteEditorIfNeeded() {
        val normalized = noteEditorText.trim()
        val initial = noteEditorInitialText.trim()
        val targetId = editingNoteId

        if (targetId == null) {
            if (normalized.isBlank()) return
            handleQuickNoteInput(normalized)
            return
        }

        if (normalized.isBlank() || normalized == initial) return
        quickNoteRepository.updateContent(targetId, normalized)
        debugLogs += "${getFormattedTime()}: Updated quick note $targetId\n"
    }

    fun closeNoteEditor(saveChanges: Boolean) {
        if (saveChanges) {
            persistNoteEditorIfNeeded()
        }
        editingNoteId = null
        noteEditorText = ""
        noteEditorInitialText = ""
        currentTab = BottomTab.Notes
        currentScreen = AppScreen.Notes
    }

    fun screenForTab(tab: BottomTab): AppScreen {
        return when (tab) {
            BottomTab.Home -> AppScreen.Home
            BottomTab.Calendar -> AppScreen.Calendar
            BottomTab.Notes -> AppScreen.Notes
        }
    }

    fun openTab(tab: BottomTab) {
        isComposerOpen = false
        currentTab = tab
        currentScreen = screenForTab(tab)
        if (tab == BottomTab.Calendar) {
            selection = CalendarDay(getToday(), DayPosition.MonthDate)
        }
    }

    // BackHandler
    com.remindr.app.ui.components.BackHandler(
        enabled = currentScreen != AppScreen.Home || isComposerOpen,
    ) {
        when {
            isComposerOpen -> isComposerOpen = false
            currentScreen == AppScreen.NoteEditor -> closeNoteEditor(saveChanges = true)
            currentScreen == AppScreen.Settings -> openTab(currentTab)
            currentScreen == AppScreen.Notes -> {
                currentScreen = when (currentTab) {
                    BottomTab.Calendar -> AppScreen.Calendar
                    BottomTab.Notes -> AppScreen.Home
                    BottomTab.Home -> AppScreen.Home
                }
                if (currentTab == BottomTab.Notes) {
                    currentTab = BottomTab.Home
                }
            }
            selection != null -> selection = null
        }
    }

    fun handleStatusChange(itemId: Long, status: ItemStatus) {
        val currentItem = repository.getItemById(itemId) ?: return
        repository.updateStatus(itemId, status)

        when (status) {
            ItemStatus.COMPLETED -> {
                scheduler?.cancel(currentItem)
                val seriesId = currentItem.parentId
                if (seriesId != null) {
                    repository.getNextOpenItemForSeries(seriesId)?.let { nextItem ->
                        scheduler?.schedule(nextItem)
                    }
                }
            }

            ItemStatus.ARCHIVED -> {
                scheduler?.cancel(currentItem)
            }

            ItemStatus.DELETED -> {
                scheduler?.cancel(currentItem)
            }

            else -> {
                val updatedItem = repository.getItemById(itemId) ?: return
                scheduler?.cancel(currentItem)
                if (updatedItem.status != ItemStatus.COMPLETED &&
                    updatedItem.status != ItemStatus.ARCHIVED &&
                    updatedItem.status != ItemStatus.DELETED
                ) {
                    scheduler?.schedule(updatedItem)
                }
            }
        }
    }

    fun openEditor(item: Item) {
        editingItemId = item.id
        editTitle = item.title
        editRecurrenceType = item.recurrenceType
        editIntervalText = item.recurrenceInterval.toString()
        editDueDateText = item.time?.date?.toUsDateString().orEmpty()
        editDueTimeText = item.time?.let { "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}" }.orEmpty()
        editEndMode = if (item.recurrenceType == null) {
            "NEVER"
        } else if (item.recurrenceEndMode == "UNTIL_DATE" && item.recurrenceEndDate != null) {
            "UNTIL_DATE"
        } else {
            "NEVER"
        }
        editEndDateText = item.recurrenceEndDate?.date?.toUsDateString().orEmpty()
    }

    fun closeEditor() {
        editingItemId = null
    }

    fun saveEdits() {
        val itemId = editingItemId ?: return
        val currentItem = repository.getItemById(itemId) ?: run {
            closeEditor()
            return
        }

        val normalizedTitle = editTitle.trim().ifBlank { currentItem.title }
        val parsedDate = parseLocalDateOrNull(editDueDateText) ?: currentItem.time?.date ?: getToday()
        val parsedTime = parseLocalTimeOrNull(editDueTimeText) ?: currentItem.time?.time ?: LocalTime(12, 0)
        val parsedDueAt = parsedDate.atTime(parsedTime)
        val parsedRecurrenceType = editRecurrenceType?.takeIf { it in supportedRecurrenceTypes }
        val parsedRecurrenceInterval = editIntervalText.trim().toIntOrNull()?.coerceAtLeast(1) ?: currentItem.recurrenceInterval
        val parsedEndDate = if (editEndMode == "UNTIL_DATE") {
            parseLocalDateOrNull(editEndDateText) ?: parsedDate
        } else {
            null
        }
        val parsedEndDateTime = parsedEndDate?.atTime(parsedTime)

        val updatedItem = currentItem.copy(
            title = normalizedTitle,
            description = currentItem.description,
            time = parsedDueAt,
            recurrenceType = parsedRecurrenceType,
            recurrenceInterval = parsedRecurrenceInterval,
            recurrenceEndMode = if (parsedRecurrenceType == null) "NEVER" else editEndMode,
            recurrenceEndDate = if (parsedRecurrenceType == null) null else parsedEndDateTime,
            reminderOffsets = listOf(0L),
        )

        repository.update(updatedItem)

        if (updatedItem.status != ItemStatus.COMPLETED &&
            updatedItem.status != ItemStatus.ARCHIVED &&
            updatedItem.status != ItemStatus.DELETED
        ) {
            scheduler?.cancel(currentItem)
            scheduler?.schedule(updatedItem)
        }

        closeEditor()
    }

    val showLoadingOverlay = isSaving

    // Main Layout
    Box(modifier = Modifier.fillMaxSize()) {
    when (currentScreen) {
        AppScreen.NoteEditor -> {
            NoteEditorScreen(
                content = noteEditorText,
                isNewNote = editingNoteId == null,
                onContentChange = { noteEditorText = it },
                onDone = { closeNoteEditor(saveChanges = true) },
                onBack = { closeNoteEditor(saveChanges = true) },
            )
        }

        AppScreen.Settings -> {
            SettingsScreen(
                apiKey = apiKey,
                versionLabel = appVersionLabel,
                onApiKeyChange = { newKey ->
                    apiKey = newKey
                    repository.saveApiKey(newKey)
                },
                onBack = { openTab(currentTab) },
            )
        }

        AppScreen.Home, AppScreen.Calendar, AppScreen.Notes -> {
            Scaffold(
                containerColor = pageBackgroundColor,
                bottomBar = {
                    Column(Modifier.imePadding()) {
                        AnimatedVisibility(
                            visible = isComposerOpen,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            InputBar(
                                placeholder = when (currentTab) {
                                    BottomTab.Home -> "New Reminder..."
                                    BottomTab.Calendar -> if (selection != null) "Add to ${selection?.date?.toUsDateString()}..." else "New Reminder..."
                                    BottomTab.Notes -> "New Reminder..."
                                },
                                isSaving = isSaving,
                                autoFocusTick = composerFocusTick,
                                onSend = { text ->
                                    handleAiInput(text)
                                    isComposerOpen = false
                                },
                            )
                        }
                        BottomActionRow(
                            selectedTab = currentTab,
                            onSelectTab = { tab ->
                                openTab(tab)
                            },
                            onQuickReminderClick = {
                                isComposerOpen = true
                                composerFocusTick += 1
                            },
                            onQuickNoteClick = {
                                openNewNoteEditor()
                            },
                            modifier = Modifier.navigationBarsPadding(),
                        )
                    }
                },
            ) { scaffoldPadding ->
                Box(modifier = Modifier.padding(scaffoldPadding)) {
                    when (currentScreen) {
                        AppScreen.Home -> {
                            HomeScreen(
                                items = items,
                                onStatusChange = { id, status -> handleStatusChange(id, status) },
                                onItemClick = { id ->
                                    repository.getItemById(id)?.let { item -> openEditor(item) }
                                },
                            )
                        }
                        AppScreen.Calendar -> {
                            CalendarScreen(
                                items = items,
                                selection = selection,
                                onSelectionChange = { selection = it },
                                recentlyAddedDate = recentlyAddedDate,
                                onItemClick = { item -> openEditor(item) },
                                onItemStatusChange = { id, status -> handleStatusChange(id, status) },
                                onSettingsClick = { currentScreen = AppScreen.Settings },
                                viewMode = viewMode,
                                onViewModeToggle = {
                                    viewMode = if (viewMode == CalendarViewMode.Month) CalendarViewMode.Year else CalendarViewMode.Month
                                },
                            )
                        }
                        AppScreen.Notes -> {
                            NotesScreen(
                                notes = notes,
                                onEditNote = { note -> openExistingNoteEditor(note) },
                                onArchiveNote = { id -> quickNoteRepository.archive(id) },
                                onDeleteNote = { id -> quickNoteRepository.delete(id) },
                                onRestoreNote = { id -> quickNoteRepository.restore(id) },
                            )
                        }
                        AppScreen.NoteEditor -> Unit
                        AppScreen.Settings -> Unit
                    }
                }
            }

            val editingItem = editingItemId?.let { id -> repository.getItemById(id) }
            if (editingItem != null) {
                val previewDate = parseLocalDateOrNull(editDueDateText) ?: editingItem.time?.date ?: getToday()
                val previewTime = parseLocalTimeOrNull(editDueTimeText) ?: editingItem.time?.time ?: LocalTime(12, 0)
                val previewDueAt = previewDate.atTime(previewTime)
                val previewRecurrenceType = editRecurrenceType?.takeIf { it in supportedRecurrenceTypes }
                val previewRecurrenceInterval = editIntervalText.trim().toIntOrNull()?.coerceAtLeast(1) ?: editingItem.recurrenceInterval
                val previewEndDate = if (editEndMode == "UNTIL_DATE") parseLocalDateOrNull(editEndDateText) else null
                val scheduleMode = when {
                    previewRecurrenceType == null -> "ONE_TIME"
                    editEndMode == "UNTIL_DATE" -> "RECURRING_UNTIL_DATE"
                    else -> "RECURRING_FOREVER"
                }
                val editablePreview = editingItem.copy(
                    title = editTitle.ifBlank { editingItem.title },
                    description = editingItem.description,
                    time = previewDueAt,
                    recurrenceType = previewRecurrenceType,
                    recurrenceInterval = previewRecurrenceInterval,
                    recurrenceEndMode = if (previewRecurrenceType == null) "NEVER" else editEndMode,
                    recurrenceEndDate = previewEndDate?.atTime(previewTime),
                    reminderOffsets = listOf(0L),
                )
                val willOccurPreview = occurrencePreviewLine(
                    dueAt = previewDueAt,
                    recurrenceType = previewRecurrenceType,
                    recurrenceInterval = previewRecurrenceInterval,
                    recurrenceEndDate = editablePreview.recurrenceEndDate,
                )
                ModalBottomSheet(
                    onDismissRequest = { closeEditor() },
                    containerColor = Colors.example5PageBgColor,
                ) {
                    EditSheet(
                        title = editTitle,
                        onTitleChange = { editTitle = it },
                        dueSummary = editablePreview.dueSummary,
                        recurrenceSummary = editablePreview.recurrenceSummary,
                        scheduleModeSummary = editablePreview.scheduleModeSummary,
                        willOccurPreview = willOccurPreview,
                        dueDate = editDueDateText,
                        onDueDateChange = { editDueDateText = it },
                        dueTime = editDueTimeText,
                        onDueTimeChange = { editDueTimeText = it },
                        scheduleMode = scheduleMode,
                        onScheduleModeChange = { mode ->
                            when (mode) {
                                "ONE_TIME" -> {
                                    editRecurrenceType = null
                                    editIntervalText = "1"
                                    editEndMode = "NEVER"
                                    editEndDateText = ""
                                }

                                "RECURRING_FOREVER" -> {
                                    if (editRecurrenceType == null) editRecurrenceType = "MONTHLY"
                                    if (editIntervalText.isBlank()) editIntervalText = "1"
                                    editEndMode = "NEVER"
                                    editEndDateText = ""
                                }

                                "RECURRING_UNTIL_DATE" -> {
                                    if (editRecurrenceType == null) editRecurrenceType = "MONTHLY"
                                    if (editIntervalText.isBlank()) editIntervalText = "1"
                                    editEndMode = "UNTIL_DATE"
                                    if (editEndDateText.isBlank()) {
                                        editEndDateText = editDueDateText
                                    }
                                }
                            }
                        },
                        recurrenceType = editRecurrenceType ?: "MONTHLY",
                        onRecurrenceTypeChange = { nextType ->
                            editRecurrenceType = nextType
                        },
                        recurrenceInterval = editIntervalText,
                        onRecurrenceIntervalChange = { editIntervalText = it.filter { c -> c.isDigit() }.ifBlank { "" } },
                        endDate = editEndDateText,
                        onEndDateChange = { editEndDateText = it },
                        onSave = { saveEdits() },
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showLoadingOverlay,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0E0E0E).copy(alpha = 0.96f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RemindrWordmark(iconSize = 44.dp, fontSize = 24.sp)
                Text(
                    text = "Loading...",
                    color = Colors.example5TextGreyLight,
                    fontSize = 14.sp,
                )
            }
        }
    }
    } // end Box
}

@Composable
private fun BottomActionRow(
    selectedTab: BottomTab,
    onSelectTab: (BottomTab) -> Unit,
    onQuickReminderClick: () -> Unit,
    onQuickNoteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SideQuickAddButton(
            icon = Icons.Default.DateRange,
            contentDescription = "Quick reminder",
            onClick = onQuickReminderClick,
        )

        BottomRadioNav(
            selectedTab = selectedTab,
            onSelect = onSelectTab,
            modifier = Modifier.weight(1f),
        )

        SideQuickAddButton(
            icon = Icons.Default.Edit,
            contentDescription = "Quick note",
            onClick = onQuickNoteClick,
        )
    }
}

@Composable
private fun BottomRadioNav(
    selectedTab: BottomTab,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(44.dp),
        shape = RoundedCornerShape(24.dp),
        color = Colors.example5ToolbarColor.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomRadioNavButton(
                selected = selectedTab == BottomTab.Home,
                icon = Icons.Default.TaskAlt,
                contentDescription = "Reminders",
                onClick = { onSelect(BottomTab.Home) },
            )
            BottomRadioNavButton(
                selected = selectedTab == BottomTab.Calendar,
                icon = Icons.Default.DateRange,
                contentDescription = "Calendar",
                onClick = { onSelect(BottomTab.Calendar) },
            )
            BottomRadioNavButton(
                selected = selectedTab == BottomTab.Notes,
                icon = Icons.Default.Description,
                contentDescription = "Notes",
                onClick = { onSelect(BottomTab.Notes) },
            )
        }
    }
}

@Composable
private fun RowScope.BottomRadioNavButton(
    selected: Boolean,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val iconTint = if (selected) Color.White else Colors.example5TextGreyLight
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SideQuickAddButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(44.dp)
            .width(54.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = Colors.example5ToolbarColor.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "+",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Colors.example5TextGreyLight,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(14.dp),
            )
        }
    }
}

private fun ParsedItem.safeDateOrNull(): LocalDate? {
    val parsedYear = year ?: return null
    val parsedMonth = month ?: return null
    val parsedDay = day ?: return null
    return runCatching { LocalDate(parsedYear, parsedMonth, parsedDay) }.getOrNull()
}

private fun ParsedItem.safeTimeOrDefault(): LocalTime {
    return LocalTime(
        hour = hour.coerceIn(0, 23),
        minute = minute.coerceIn(0, 59),
    )
}

private fun ParsedItem?.safeRecurrenceEndDateOrNull(): LocalDateTime? {
    if (this == null) return null
    val endYear = recurrenceEndYear ?: return null
    val endMonth = recurrenceEndMonth ?: return null
    return runCatching {
        LocalDate(endYear, endMonth, 1).atTime(0, 0)
    }.getOrNull()
}

private fun inferRecurrenceTypeFromInput(input: String): String? {
    val lower = input.lowercase()
    return when {
        Regex("\\b(daily|every day|each day)\\b").containsMatchIn(lower) -> "DAILY"
        Regex("\\b(weekly|every week|each week|every monday|every tuesday|every wednesday|every thursday|every friday|every saturday|every sunday|biweekly|every two weeks)\\b").containsMatchIn(lower) -> "WEEKLY"
        Regex("\\b(monthly|every month|each month|per month)\\b").containsMatchIn(lower) -> "MONTHLY"
        Regex("\\b(yearly|annually|every year|each year)\\b").containsMatchIn(lower) -> "YEARLY"
        else -> null
    }
}

private fun resolveRecurrenceTypeForInput(
    parsedItem: ParsedItem?,
    rawInput: String,
    explicitDate: LocalDate?,
): String? {
    val inferred = inferRecurrenceTypeFromInput(rawInput)
    if (inferred != null) return inferred

    val parsed = parsedItem?.recurrenceType?.uppercase()?.takeIf { it in supportedRecurrenceTypes } ?: return null
    val hasCue = rawInput.hasRecurrenceCue()
    if (explicitDate != null && !hasCue) return null
    if (!hasCue) return null
    return parsed
}

private fun resolveDateForInput(
    parsedItem: ParsedItem?,
    rawInput: String,
    today: LocalDate,
    recurrenceType: String?,
    explicitDate: LocalDate?,
): LocalDate {
    val parsedDate = parsedItem?.safeDateOrNull() ?: explicitDate
    if (recurrenceType != "MONTHLY") {
        return parsedDate ?: today
    }

    if (parsedDate != null && parsedDate >= today && rawInput.hasExplicitYearSignal()) {
        return parsedDate
    }

    val inferredDayOfMonth = inferDayOfMonthFromInput(rawInput)
        ?: if (rawInput.hasExplicitDateSignal()) parsedDate?.day else null
    val dayOfMonth = (inferredDayOfMonth ?: 1).coerceIn(1, 31)

    return nextMonthlyDate(dayOfMonth, today)
}

private fun resolveTitleForInput(parsedItem: ParsedItem?, rawInput: String): String {
    val fallback = rawInput
        .trim()
        .lineSequence()
        .firstOrNull()
        ?.trim()
        ?.take(80)
        .orEmpty()
        .ifBlank { "Reminder" }
    val aiTitle = parsedItem?.title?.trim().orEmpty()
    return when {
        aiTitle.isBlank() -> fallback
        aiTitle.equals("Reminder", ignoreCase = true) -> fallback
        else -> aiTitle
    }
}

private fun extractExplicitDateFromInput(input: String, today: LocalDate): LocalDate? {
    val lower = input.lowercase()

    Regex("\\b(\\d{4})-(\\d{1,2})-(\\d{1,2})\\b").find(lower)?.let { match ->
        val year = match.groupValues[1].toIntOrNull() ?: return@let
        val month = match.groupValues[2].toIntOrNull() ?: return@let
        val day = match.groupValues[3].toIntOrNull() ?: return@let
        return runCatching { LocalDate(year, month, day) }.getOrNull()
    }

    val monthMap = mapOf(
        "jan" to 1, "january" to 1,
        "feb" to 2, "february" to 2,
        "mar" to 3, "march" to 3,
        "apr" to 4, "april" to 4,
        "may" to 5,
        "jun" to 6, "june" to 6,
        "jul" to 7, "july" to 7,
        "aug" to 8, "august" to 8,
        "sep" to 9, "sept" to 9, "september" to 9,
        "oct" to 10, "october" to 10,
        "nov" to 11, "november" to 11,
        "dec" to 12, "december" to 12,
    )

    Regex("\\b(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t|tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\.?\\s+([0-9]{1,2})(?:st|nd|rd|th)?(?:,?\\s+([0-9]{4}))?\\b")
        .find(lower)
        ?.let { match ->
            val monthKey = match.groupValues[1]
            val month = monthMap[monthKey] ?: return@let
            val day = match.groupValues[2].toIntOrNull() ?: return@let
            val explicitYear = match.groupValues.getOrNull(3)?.toIntOrNull()
            if (explicitYear != null) {
                return runCatching { LocalDate(explicitYear, month, day) }.getOrNull()
            }
            val currentYearCandidate = runCatching { LocalDate(today.year, month, day) }.getOrNull()
            if (currentYearCandidate != null && currentYearCandidate >= today) return currentYearCandidate
            return runCatching { LocalDate(today.year + 1, month, day) }.getOrNull()
        }

    Regex("\\b([0-9]{1,2})/([0-9]{1,2})(?:/([0-9]{2,4}))?\\b").find(lower)?.let { match ->
        val month = match.groupValues[1].toIntOrNull() ?: return@let
        val day = match.groupValues[2].toIntOrNull() ?: return@let
        val rawYear = match.groupValues.getOrNull(3)?.toIntOrNull()
        val year = when {
            rawYear == null -> {
                val candidate = runCatching { LocalDate(today.year, month, day) }.getOrNull()
                if (candidate != null && candidate >= today) today.year else today.year + 1
            }
            rawYear < 100 -> 2000 + rawYear
            else -> rawYear
        }
        return runCatching { LocalDate(year, month, day) }.getOrNull()
    }

    return null
}

private fun inferDayOfMonthFromInput(input: String): Int? {
    val lower = input.lowercase()
    val ordinalWordMap = mapOf(
        "first" to 1,
        "second" to 2,
        "third" to 3,
        "fourth" to 4,
        "fifth" to 5,
        "sixth" to 6,
        "seventh" to 7,
        "eighth" to 8,
        "ninth" to 9,
        "tenth" to 10,
        "eleventh" to 11,
        "twelfth" to 12,
        "thirteenth" to 13,
        "fourteenth" to 14,
        "fifteenth" to 15,
        "sixteenth" to 16,
        "seventeenth" to 17,
        "eighteenth" to 18,
        "nineteenth" to 19,
        "twentieth" to 20,
        "twenty-first" to 21,
        "twenty-second" to 22,
        "twenty-third" to 23,
        "twenty-fourth" to 24,
        "twenty-fifth" to 25,
        "twenty-sixth" to 26,
        "twenty-seventh" to 27,
        "twenty-eighth" to 28,
        "twenty-ninth" to 29,
        "thirtieth" to 30,
        "thirty-first" to 31,
    )

    ordinalWordMap.forEach { (word, value) ->
        if (Regex("\\b$word\\b").containsMatchIn(lower)) return value
    }

    val explicitOrdinal = Regex("\\b([1-9]|[12][0-9]|3[01])(st|nd|rd|th)\\b")
        .find(lower)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()

    return explicitOrdinal
}

private fun nextMonthlyDate(dayOfMonth: Int, today: LocalDate): LocalDate {
    var year = today.year
    var month = today.month.number
    val preferredDay = dayOfMonth.coerceIn(1, 31)

    var candidate = localDateOrLastValidDay(year, month, preferredDay)
    if (candidate < today) {
        month += 1
        if (month > 12) {
            month = 1
            year += 1
        }
        candidate = localDateOrLastValidDay(year, month, preferredDay)
    }

    return candidate
}

private fun localDateOrLastValidDay(year: Int, month: Int, preferredDay: Int): LocalDate {
    for (day in preferredDay downTo 1) {
        val date = runCatching { LocalDate(year, month, day) }.getOrNull()
        if (date != null) return date
    }
    return LocalDate(year, month, 1)
}

private fun String.hasExplicitDateSignal(): Boolean {
    val lower = lowercase()
    val monthNamePattern = Regex("\\b(january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)\\b")
    val ordinalPattern = Regex("\\b([1-9]|[12][0-9]|3[01])(st|nd|rd|th)\\b")
    val slashPattern = Regex("\\b([0-9]{1,2})/([0-9]{1,2})(?:/([0-9]{2,4}))?\\b")
    val isoPattern = Regex("\\b(\\d{4})-(\\d{1,2})-(\\d{1,2})\\b")
    return monthNamePattern.containsMatchIn(lower) ||
        ordinalPattern.containsMatchIn(lower) ||
        slashPattern.containsMatchIn(lower) ||
        isoPattern.containsMatchIn(lower)
}

private fun String.hasExplicitYearSignal(): Boolean {
    return Regex("\\b(19|20)\\d{2}\\b").containsMatchIn(this)
}

private fun String.hasRecurrenceCue(): Boolean {
    val lower = lowercase()
    return Regex("\\b(every|each|daily|weekly|monthly|yearly|annually|biweekly|repeat|repeats|recurring)\\b")
        .containsMatchIn(lower)
}

private val supportedRecurrenceTypes = setOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")

private fun seedMockData(
    repository: ItemRepository,
    quickNoteRepository: QuickNoteRepository,
) {
    val now = getCurrentDateTime()
    val today = getToday()

    fun mockReminder(
        title: String,
        description: String,
        daysFromToday: Int,
        hour: Int,
        minute: Int,
        status: ItemStatus = ItemStatus.PENDING,
        recurrenceType: String? = null,
        recurrenceInterval: Int = 1,
        recurrenceEndDate: LocalDateTime? = null,
    ): Item {
        val dueAt = today.plus(DatePeriod(days = daysFromToday)).atTime(hour, minute)
        val endMode = when {
            recurrenceType == null -> "NEVER"
            recurrenceEndDate != null -> "UNTIL_DATE"
            else -> "NEVER"
        }
        return Item(
            title = title,
            description = description,
            time = dueAt,
            color = Colors.example5TextGrey,
            status = status,
            recurrenceType = recurrenceType,
            recurrenceInterval = recurrenceInterval,
            recurrenceEndMode = endMode,
            recurrenceEndDate = recurrenceEndDate,
            reminderOffsets = listOf(1440L, 60L, 0L),
            createdAt = now,
        )
    }

    val reminders = listOf(
        mockReminder(
            title = "Discover Card Payment",
            description = "Pay statement balance in full.",
            daysFromToday = 11,
            hour = 8,
            minute = 0,
            recurrenceType = "MONTHLY",
        ),
        mockReminder(
            title = "Rent",
            description = "Transfer rent to landlord.",
            daysFromToday = 9,
            hour = 9,
            minute = 0,
            recurrenceType = "MONTHLY",
        ),
        mockReminder(
            title = "Water Bill",
            description = "City utilities autopay check.",
            daysFromToday = 18,
            hour = 18,
            minute = 30,
            recurrenceType = "MONTHLY",
        ),
        mockReminder(
            title = "Team Standup",
            description = "Weekly project check-in.",
            daysFromToday = 1,
            hour = 10,
            minute = 0,
            recurrenceType = "WEEKLY",
        ),
        mockReminder(
            title = "Daily Medication",
            description = "Take evening meds.",
            daysFromToday = 0,
            hour = 20,
            minute = 30,
            recurrenceType = "DAILY",
        ),
        mockReminder(
            title = "Car Insurance Renewal",
            description = "Review premium and renew if needed.",
            daysFromToday = 45,
            hour = 12,
            minute = 0,
            recurrenceType = "YEARLY",
        ),
        mockReminder(
            title = "Quarterly Tax Estimate",
            description = "Submit quarterly payment.",
            daysFromToday = 26,
            hour = 15,
            minute = 0,
            recurrenceType = "MONTHLY",
            recurrenceInterval = 3,
        ),
        mockReminder(
            title = "Submit Expense Report",
            description = "Last week's receipts.",
            daysFromToday = -2,
            hour = 14,
            minute = 15,
            status = ItemStatus.COMPLETED,
        ),
        mockReminder(
            title = "Review 401k Allocation",
            description = "Portfolio rebalance note.",
            daysFromToday = -7,
            hour = 11,
            minute = 0,
            status = ItemStatus.ARCHIVED,
        ),
        mockReminder(
            title = "Cancel Old Trial Services",
            description = "Old subscriptions no longer needed.",
            daysFromToday = -20,
            hour = 16,
            minute = 45,
            status = ItemStatus.DELETED,
        ),
    )

    reminders.forEach(repository::insert)

    quickNoteRepository.insert(
        "- Grocery run tomorrow\n- Eggs\n- Avocados\n- Coffee beans",
    )
    quickNoteRepository.insert(
        "- Ideas for Q2\n  - Better onboarding flow\n  - Simplify settings copy",
    )
    val archivedId = quickNoteRepository.insert(
        "Archived draft: vacation packing checklist",
    )
    val deletedId = quickNoteRepository.insert(
        "Deleted draft: random scratch notes",
    )
    quickNoteRepository.insert(
        "- Meeting notes\n  - Ship Android polish\n  - Date format cleanup",
    )

    if (archivedId != null) quickNoteRepository.archive(archivedId)
    if (deletedId != null) quickNoteRepository.delete(deletedId)
}

private fun occurrencePreviewLine(
    dueAt: LocalDateTime,
    recurrenceType: String?,
    recurrenceInterval: Int,
    recurrenceEndDate: LocalDateTime?,
): String {
    val timeText = formatTime12(dueAt.time)
    if (recurrenceType == null) {
        return "Will occur on ${dueAt.date.toUsDateString()} at $timeText."
    }

    val every = recurrenceInterval.coerceAtLeast(1)
    val unit = when (recurrenceType) {
        "DAILY" -> "day"
        "WEEKLY" -> "week"
        "MONTHLY" -> "month"
        "YEARLY" -> "year"
        else -> "cycle"
    }
    val cadence = if (every == 1) "every $unit" else "every $every ${unit}s"
    val until = recurrenceEndDate?.let { " until ${it.date.toUsDateString()}" } ?: ""
    return "Will occur on ${dueAt.date.toUsDateString()} at $timeText, then $cadence$until."
}

private fun parseLocalDateOrNull(input: String): LocalDate? {
    return parseUsOrIsoLocalDateOrNull(input)
}

private fun parseLocalTimeOrNull(input: String): LocalTime? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null
    val match = Regex("^(\\d{1,2}):(\\d{2})$").find(trimmed) ?: return null
    val hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return LocalTime(hour, minute)
}
