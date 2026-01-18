package com.kizitonwose.calendar.compose.multiplatform.sample

import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import kotlinx.datetime.Month
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import kotlinx.datetime.YearMonth
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.*
import androidx.compose.foundation.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.zIndex
// Removed java.time imports to fix type mismatch
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime

import com.kizitonwose.calendar.compose.multiplatform.sample.rememberSpeechRecognizer
import com.kizitonwose.calendar.sample.db.RemindrDatabase
import com.kizitonwose.calendar.compose.multiplatform.sample.DatabaseDriverFactory
import com.kizitonwose.calendar.compose.multiplatform.sample.NoteDbHelper
import com.kizitonwose.calendar.sample.db.QueueNote
import com.kizitonwose.calendar.compose.multiplatform.sample.SimpleCalendarTitle
import com.kizitonwose.calendar.compose.multiplatform.sample.rememberFirstCompletelyVisibleMonth
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.*

// Extensions
fun YearMonth.atStartOfMonth(): LocalDate = LocalDate(year, month, 1)
fun YearMonth.atEndOfMonth(): LocalDate {
    val start = atStartOfMonth()
    val nextMonth = start.plus(1, DateTimeUnit.MONTH)
    val end = nextMonth.minus(1, DateTimeUnit.DAY)
    return end
}
// Note: lengthOfMonth not strictly needed if we implement atEndOfMonth correctly, 
// but if used elsewhere, here it is:
fun YearMonth.lengthOfMonth(): Int = atEndOfMonth().dayOfMonth

private val pageBackgroundColor: Color = Colors.example5PageBgColor
private val itemBackgroundColor: Color = Colors.example5ItemViewBgColor
private val toolbarColor: Color = Colors.example5ToolbarColor
private val selectedItemColor: Color = Colors.example5TextGrey
private val inActiveTextColor: Color = Colors.example5TextGreyLight

// noteColors moved to Colors.kt

enum class Screen {
    Calendar, Settings
}

enum class Tab { Calendar, Notes }
enum class CalendarViewMode { Year, Month }
enum class MainScreen { Home, View }

@Composable
fun CalendarApp(
    driverFactory: DatabaseDriverFactory,
    requestMagicAdd: Boolean = false,
    scheduler: ReminderScheduler? = null,
    onRequestNotificationTest: ((String) -> Unit) -> Unit,
    onRequestRichNotificationTest: ((String) -> Unit) -> Unit
) {
    var screen by remember { mutableStateOf(Screen.Calendar) }
    var currentTab by remember { mutableStateOf(Tab.Calendar) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.Month) }
    var showAddDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    // Database Init
    val database = remember { RemindrDatabase(driverFactory.createDriver()) }
    val dbHelper = remember { NoteDbHelper(database) }
    
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(500) }
    val endMonth = remember { currentMonth.plusMonths(500) }
    
    var selection by remember { mutableStateOf<CalendarDay?>(null) }
    val daysOfWeek = remember { daysOfWeek() }
    
    // Feature: Highlight recently added note
    var recentlyAddedDate by remember { mutableStateOf<LocalDate?>(null) }
    
    // Clear highlight after a delay
    LaunchedEffect(recentlyAddedDate) {
        if (recentlyAddedDate != null) {
            kotlinx.coroutines.delay(2000) // Highlight for 2 seconds
            recentlyAddedDate = null
        }
    }

    // Load notes from DB
    val notes = dbHelper.getAllNotes().collectAsState(initial = emptyList()).value
    val queueNotes = dbHelper.getQueueNotes().collectAsState(initial = emptyList()).value
    
    var apiKey by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        dbHelper.getApiKey()?.let { apiKey = it }
    }

    // State for Magic/Manual Sheet
    // 0 = Closed, 1 = Magic, 2 = Manual (reusing existing logic slightly modified)
    var sheetMode by remember { mutableStateOf(0) } // 0: Closed, 1: Magic, 2: Manual
    
    // Magic Add Auto-Trigger
    LaunchedEffect(requestMagicAdd) {
        if (requestMagicAdd) sheetMode = 1
    }

    val notesInSelectedDate = remember(notes, selection) {
        derivedStateOf {
            val date = selection?.date
            if (date == null) emptyList() else notes.filter { it.time.date == date }
        }
    }
    
    // Hoisted State for Manual View
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedColor by remember { mutableStateOf(Colors.noteColors.first()) }
    var selectedTime by remember { mutableStateOf(LocalTime(8, 0)) }
    
    // New State for Reminders
    var reminderOffsets by remember { mutableStateOf<List<Long>>(emptyList()) }
    var severity by remember { mutableStateOf(Severity.MEDIUM) }
    var recurrenceType by remember { mutableStateOf<String?>(null) }
    var nagEnabled by remember { mutableStateOf(false) }
    var editingNoteId by remember { mutableStateOf<Long?>(null) }

    // Logs state
    var debugLogs by remember { mutableStateOf("Logs will appear here...\n") }

    when (screen) {
        Screen.Settings -> {
            SettingsScreen(
                apiKey = apiKey,
                onApiKeyChange = { newKey ->
                    apiKey = newKey
                    dbHelper.saveApiKey(newKey)
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
                onBack = { screen = Screen.Calendar }
            )
        }
        Screen.Calendar -> {
            // Hoisted State for Overlay & Calendar
            val snackbarHostState = remember { SnackbarHostState() }
            val aiService = remember { AIService() }
            var isThinking by remember { mutableStateOf(false) }
            var magicError by remember { mutableStateOf<String?>(null) }

            val state = rememberCalendarState(
                startMonth = startMonth,
                endMonth = endMonth,
                firstVisibleMonth = currentMonth,
                firstDayOfWeek = daysOfWeek.first(),
                outDateStyle = OutDateStyle.EndOfGrid,
            )
            
            // weekState removed
            
            val visibleMonth = rememberFirstCompletelyVisibleMonth(state)
            LaunchedEffect(visibleMonth) {
                selection = null
            }

            BackHandler(enabled = selection != null || screen == Screen.Settings) {
                 if (screen == Screen.Settings) {
                     screen = Screen.Calendar
                 } else {
                     selection = null
                 }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = pageBackgroundColor,
                    topBar = {
                        CompositionLocalProvider(LocalContentColor provides Color.White) {
                            SimpleCalendarTitle(
                                modifier = Modifier
                                    .background(toolbarColor)
                                    .padding(horizontal = 8.dp, vertical = 12.dp)
                                    .applyScaffoldTopPaddingLocal(),
                                currentMonth = visibleMonth.yearMonth,
                                goToPrevious = {
                                    coroutineScope.launch {
                                        state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.previous)
                                    }
                                },
                                goToNext = {
                                    coroutineScope.launch {
                                        state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.next)
                                    }
                                },
                                onSettingsClick = { screen = Screen.Settings },
                                onTitleClick = { viewMode = if (viewMode == CalendarViewMode.Month) CalendarViewMode.Year else CalendarViewMode.Month }
                            )
                        }
                    },
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .padding(bottom = 80.dp)
                                .zIndex(2f)
                        )
                    }
                ) { innerPadding ->
                     // ALWAYS SHOW CALENDAR
                     Column(
                          modifier = Modifier
                              .fillMaxSize()
                              .padding(innerPadding)
                              .applyScaffoldHorizontalPaddingsLocal()
                     ) {
                         // Content moved here

                        
                        CompositionLocalProvider(LocalContentColor provides Color.White) {
                            // View Mode Switcher Removed

                            AnimatedContent(
                                targetState = viewMode,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                                }
                            ) { mode ->
                                when (mode) {
                                    CalendarViewMode.Month -> {
                                        HorizontalCalendar(
                                            modifier = Modifier.wrapContentWidth(),
                                            state = state,
                                            dayContent = { day ->
                                                val notesForDay = if (day.position == DayPosition.MonthDate) {
                                                    notes.filter { it.time.date == day.date }
                                                } else {
                                                    emptyList()
                                                }
                                                Day(
                                                    day = day,
                                                    isSelected = selection == day,
                                                    isHighlighted = day.date == recentlyAddedDate,
                                                    notes = notesForDay,
                                                ) { clicked ->
                                                    selection = clicked
                                                }
                                            },
                                            monthHeader = {
                                                MonthHeader(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    daysOfWeek = daysOfWeek,
                                                )
                                            },
                                        )
                                    }
                                    CalendarViewMode.Year -> {
                                        val initialPage = Int.MAX_VALUE / 2
                                        val pagerState = rememberPagerState(initialPage = initialPage) { Int.MAX_VALUE }
                                        
                                        HorizontalPager(
                                            state = pagerState,
                                            modifier = Modifier.fillMaxSize()
                                        ) { page ->
                                            val yearOffset = page - initialPage
                                            val year = currentMonth.year + yearOffset
                                            
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                Text(
                                                    text = year.toString(),
                                                    style = MaterialTheme.typography.headlineMedium,
                                                    color = Color.White,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 16.dp),
                                                    textAlign = TextAlign.Center,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                
                                                LazyVerticalGrid(
                                                    columns = GridCells.Fixed(3),
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentPadding = PaddingValues(16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                ) {
                                                    items(12) { index ->
                                                        val monthName = Month.values()[index].name.take(3)
                                                        Box(
                                                            modifier = Modifier
                                                                .aspectRatio(1f)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(
                                                                    if (year == currentMonth.year && index == currentMonth.month.ordinal) 
                                                                        selectedItemColor.copy(alpha = 0.5f) 
                                                                    else 
                                                                        Color.DarkGray
                                                                )
                                                                .clickable {
                                                                    coroutineScope.launch {
                                                                        state.scrollToMonth(YearMonth(year, Month.values()[index]))
                                                                        viewMode = CalendarViewMode.Month
                                                                    }
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(text = monthName, color = Color.White, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            


                            HorizontalDivider(color = pageBackgroundColor)
                            
                            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                if (currentTab == Tab.Calendar) {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(items = notesInSelectedDate.value) { note ->
                                            Box(
                                                modifier = Modifier
                                                    .clickable {
                                                        // Populate Editor for Editing
                                                        title = note.title
                                                        description = note.description ?: ""
                                                        endDate = note.endDate?.date
                                                        selectedTime = note.time.time
                                                        selectedColor = note.color
                                                        severity = note.severity
                                                        reminderOffsets = note.reminderOffsets
                                                        recurrenceType = note.recurrenceType
                                                        nagEnabled = note.nagEnabled
                                                        editingNoteId = note.id
                                                        sheetMode = 2
                                                    }
                                            ) {
                                                NoteInformation(
                                                    note = note, 
                                                    onDelete = { 
                                                        dbHelper.deleteById(note.id)
                                                        scheduler?.cancel(note)
                                                    },
                                                    onComplete = { isComplete ->
                                                        dbHelper.updateCompletion(note.id, isComplete)
                                                        if (isComplete) {
                                                            scheduler?.cancel(note)
                                                            
                                                            // Handle Recurrence (Create next note)
                                                            if (note.recurrenceType != null) {
                                                                val nextDate = RecurrenceEngine.getNextDueDate(note)
                                                                if (nextDate != null) {
                                                                    val newNote = note.copy(
                                                                        id = -1, // New ID
                                                                        time = nextDate,
                                                                        isCompleted = false,
                                                                        lastCompletedAt = null // Reset for new instance
                                                                    )
                                                                    dbHelper.insert(newNote)
                                                                    val newId = dbHelper.getLastInsertedNoteId()
                                                                    if (newId != null) {
                                                                        scheduler?.schedule(newNote.copy(id = newId))
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            // Un-completing? Schedule again if future?
                                                            // Or if it was Nag, schedule again?
                                                            if (note.nagEnabled || note.reminderOffsets.isNotEmpty()) {
                                                                scheduler?.schedule(note)
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(items = queueNotes) { note ->
                                            QueueNoteInformation(note, onDelete = { dbHelper.deleteQueueById(note.id) })
                                        }
                                    }
                                }
                            }
                        }


                        }
                     } // End Main Column
                     


                     

                  } // End Scaffold Content Scope





                        // Manual Overlay Sheet
                        AnimatedVisibility(
                            visible = sheetMode == 2,
                            enter = fadeIn() + slideInVertically { it },
                            exit = fadeOut() + slideOutVertically { it },
                            modifier = Modifier.fillMaxSize().zIndex(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .imePadding()
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable { sheetMode = 0 }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(
                                             color = pageBackgroundColor,
                                             shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                        )
                                        .clickable(enabled = false) {}
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                      EditNoteSheet(
                                          title = title,
                                          onTitleChange = { title = it },
                                          description = description,
                                          onDescriptionChange = { description = it },
                                          endDate = endDate,
                                          onEndDateChange = { endDate = it },
                                          selectedColor = selectedColor,
                                          selectedTime = selectedTime,
                                          onColorChange = { selectedColor = it },
                                          onTimeChange = { selectedTime = it },
                                          reminderOffsets = reminderOffsets,
                                          recurrenceType = recurrenceType,
                                          nagEnabled = nagEnabled,
                                          severity = severity,
                                          onReminderOffsetsChange = { reminderOffsets = it },
                                          onRecurrenceTypeChange = { recurrenceType = it },
                                          onNagEnabledChange = { nagEnabled = it },
                                          onSeverityChange = { severity = it },
                                          onSave = {
                                              val date = selection?.date ?: getToday()
                                              val note = CalendarNote(
                                                  id = editingNoteId ?: -1L,
                                                  time = date.atTime(selectedTime),
                                                  title = title,
                                                  description = description,
                                                  endDate = endDate?.atTime(23, 59),
                                                  color = selectedColor,
                                                  reminderOffsets = reminderOffsets,
                                                  recurrenceType = recurrenceType,
                                                  nagEnabled = nagEnabled,
                                                  severity = severity
                                              )
                                              
                                              if (editingNoteId != null) {
                                                  dbHelper.update(note)
                                                  // TODO: Cancel old alarms? For now we just schedule new ones.
                                              } else {
                                                  dbHelper.insert(note)
                                              }
                                              
                                              // For scheduling, we need the ID.
                                              val finalId = if (editingNoteId != null) editingNoteId!! else dbHelper.getLastInsertedNoteId()
                                              
                                              // Schedule Reminder
                                              if (finalId != null && reminderOffsets.isNotEmpty()) {
                                                   scheduler?.schedule(note.copy(id = finalId))
                                              }
                                              
                                              
                                              title = ""
                                              description = ""
                                              endDate = null
                                              reminderOffsets = emptyList()
                                              recurrenceType = null
                                              nagEnabled = false
                                              severity = Severity.MEDIUM
                                              editingNoteId = null // Reset Edit Mode
                                              sheetMode = 0
                                              
                                              if (selection == null) {
                                                  selection = CalendarDay(date, DayPosition.MonthDate)
                                              }
                                              recentlyAddedDate = date
                                              
                                              val targetMonth = YearMonth(date.year, date.month)
                                              coroutineScope.launch {
                                                  if (targetMonth != state.firstVisibleMonth.yearMonth) {
                                                      state.animateScrollToMonth(targetMonth)
                                                  }
                                                  
                                                  // No undo for update currently
                                              }
                                          }
                                      )
                                }
                            }
                        }

            // 2. Floating Overlay Input Bar (Wrapped in Box)
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                        .background(pageBackgroundColor)
                        .animateContentSize()
                ) {
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // A. View Switcher
                        Row(
                            modifier = Modifier
                               .background(Color(0xFF333333), RoundedCornerShape(20.dp))
                               .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { currentTab = Tab.Calendar }, modifier = Modifier.size(36.dp).background(if (currentTab == Tab.Calendar) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)) {
                                Icon(Icons.Default.DateRange, "Calendar", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { currentTab = Tab.Notes }, modifier = Modifier.size(36.dp).background(if (currentTab == Tab.Notes) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)) {
                                Icon(Icons.Default.Menu, "Notes", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }

                        // B. Persistent Input Bar
                        var inputText by remember { mutableStateOf("") }
                        var isSaving by remember { mutableStateOf(false) }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text(if (currentTab == Tab.Calendar) "New Reminder..." else "New Note...", color = Color.Gray, fontSize = 14.sp) },
                            modifier = Modifier.weight(1f).heightIn(min = 40.dp, max = 100.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E1E1E),
                                unfocusedContainerColor = Color(0xFF1E1E1E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                                } else if (inputText.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            if (isSaving) return@IconButton
                                            isSaving = true
                                            coroutineScope.launch {
                                                try {
                                                    debugLogs += "${getFormattedTime()}: Requesting AI for: '$inputText'\n"
                                                    if (currentTab == Tab.Calendar) {
                                                        val today = getToday()
                                                        var parsedNote: ParsedNote? = null
                                                        if (apiKey.isNotBlank()) {
                                                            try {
                                                                parsedNote = aiService.parseNote(inputText, apiKey, today)
                                                                debugLogs += "${getFormattedTime()}: AI Response: $parsedNote\n"
                                                            } catch (e: Exception) {
                                                                debugLogs += "${getFormattedTime()}: AI Error: ${e.message}\n"
                                                            }
                                                        }
                                                        val title = parsedNote?.title ?: "Reminder"
                                                        val desc = parsedNote?.description ?: inputText
                                                        val noteDate = if (parsedNote?.year != null) LocalDate(parsedNote.year, parsedNote.month!!, parsedNote.day!!) else today
                                                        val noteTime = if (parsedNote != null) LocalTime(parsedNote.hour, parsedNote.minute) else LocalTime(12, 0)
                                                        val newNote = CalendarNote(
                                                            id = -1L, title = title, description = desc, time = noteDate.atTime(noteTime), endDate = null,
                                                            color = if (parsedNote != null) Colors.noteColors.getOrElse(parsedNote.colorIndex) { Colors.accent } else Colors.accent,
                                                            isCompleted = false, recurrenceType = parsedNote?.recurrenceType, recurrenceRule = null, nagEnabled = parsedNote?.nagEnabled ?: false,
                                                            lastCompletedAt = null, snoozedUntil = null, severity = if (parsedNote?.severity != null) Severity.valueOf(parsedNote.severity) else Severity.MEDIUM,
                                                            reminderOffsets = parsedNote?.reminderOffsets ?: listOf(0L)
                                                        )
                                                        dbHelper.insert(newNote)
                                                        selection = CalendarDay(noteDate, DayPosition.MonthDate)
                                                        val targetMonth = YearMonth(noteDate.year, noteDate.month)
                                                        if (targetMonth != state.firstVisibleMonth.yearMonth) {
                                                             state.animateScrollToMonth(targetMonth)
                                                        }
                                                    } else {
                                                        dbHelper.insertQueue(inputText)
                                                        currentTab = Tab.Notes
                                                    }
                                                    inputText = ""
                                                } finally {
                                                    isSaving = false
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ArrowForward, "Save", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        )
                    }
                }
            }
            }

            // Removed duplicate SnackbarHost from here as it is now in Scaffold
    }
}

@Composable
private fun EditNoteSheet(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    endDate: LocalDate?,
    onEndDateChange: (LocalDate?) -> Unit,
    selectedColor: Color,
    selectedTime: LocalTime,
    onColorChange: (Color) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    reminderOffsets: List<Long>,
    recurrenceType: String?,
    nagEnabled: Boolean,
    severity: Severity,
    onReminderOffsetsChange: (List<Long>) -> Unit,
    onRecurrenceTypeChange: (String?) -> Unit,
    onNagEnabledChange: (Boolean) -> Unit,
    onSeverityChange: (Severity) -> Unit,
    onSave: () -> Unit
) {
    val timePresets = listOf(
        LocalTime(8, 0),
        LocalTime(12, 0),
        LocalTime(15, 0),
        LocalTime(18, 0),
        LocalTime(20, 0)
    )
    val focusRequester = remember { FocusRequester() }

    val launchSpeech = rememberSpeechRecognizer { spokenText ->
        val separator = if (description.isBlank()) "" else " "
        onDescriptionChange(description + separator + spokenText)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        // Header
        Text(
            "Note Details", 
            style = MaterialTheme.typography.titleLarge, 
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        // 1. Title Input
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title", color = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedIndicatorColor = selectedColor,
                unfocusedIndicatorColor = Color.Gray
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        
        // 2. Description Input
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Details", color = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedIndicatorColor = selectedColor,
                unfocusedIndicatorColor = Color.Gray
            ),
            trailingIcon = {
                 IconButton(onClick = { launchSpeech() }) {
                     Icon(Icons.Filled.Mic, "Voice", tint = selectedColor)
                 }
            },
            maxLines = 5
        )

        // 2. Time & Recurrence (When)
        Card(
            colors = CardDefaults.cardColors(containerColor = itemBackgroundColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Time", color = Color.Gray, fontSize = 14.sp)
                }
                
                // Formatted Time Display (Ideally a picker, using Presets for now)
                val formattedTime = "${selectedTime.hour}:${selectedTime.minute.toString().padStart(2, '0')}"
                Text(formattedTime, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    timePresets.forEach { time ->
                        val isSelected = selectedTime == time
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTimeChange(time) },
                            label = { 
                                val label = when(time.hour) {
                                    8 -> "Morning"
                                    12 -> "Noon"
                                    15 -> "Afternoon"
                                    18 -> "Evening"
                                    20 -> "Night"
                                    else -> "${time.hour}:${time.minute}"
                                }
                                Text(label) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = selectedColor,
                                selectedLabelColor = Color.White,
                                containerColor = Color.Black.copy(alpha = 0.3f),
                                labelColor = Color.White
                            )
                        )
                    }
                }
                
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                
                // Recurrence
                 Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Repeat: ${recurrenceType ?: "Never"}", color = Color.White)
                    }
                    
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text("Change", color = selectedColor)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf(null, "DAILY", "WEEKLY", "MONTHLY").forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Never") },
                                    onClick = { 
                                        onRecurrenceTypeChange(type)
                                        expanded = false 
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Importance & Reminders (How)
        Card(
            colors = CardDefaults.cardColors(containerColor = itemBackgroundColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Notifications", color = Color.Gray, fontSize = 14.sp)
                }

                // Severity
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                     Severity.entries.forEach { level ->
                         val isSelected = severity == level
                         val (color, label) = when(level) {
                             Severity.HIGH -> Color.Red to "High / Loud"
                             Severity.MEDIUM -> Color.Yellow to "Medium"
                             Severity.LOW -> Color.Blue to "Low / Silent"
                         }
                         
                         FilterChip(
                             selected = isSelected,
                             onClick = { onSeverityChange(level) },
                             label = { Text(level.name) },
                             colors = FilterChipDefaults.filterChipColors(
                                 selectedContainerColor = color.copy(alpha = 0.8f),
                                 selectedLabelColor = Color.White,
                                 containerColor = Color.Black.copy(alpha = 0.3f),
                                 labelColor = Color.White
                             ),
                             modifier = Modifier.weight(1f)
                         )
                    }
                }
                
                // Offsets
                Text("Remind me:", color = Color.White, fontSize = 14.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                      val offsets = listOf(0L to "At time", 10L to "10m before", 30L to "30m", 60L to "1h", 1440L to "1d")
                      offsets.forEach { (offset, label) ->
                          val isSelected = reminderOffsets.contains(offset)
                          FilterChip(
                              selected = isSelected,
                              onClick = { 
                                  val newOffsets = if (isSelected) reminderOffsets - offset else reminderOffsets + offset
                                  onReminderOffsetsChange(newOffsets)
                              },
                              label = { Text(label) },
                              colors = FilterChipDefaults.filterChipColors(
                                  selectedContainerColor = selectedColor,
                                  selectedLabelColor = Color.White,
                                  containerColor = Color.Black.copy(alpha = 0.3f),
                                  labelColor = Color.White
                              )
                          )
                      }
                }
                
                // Nag
                 Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Nagging Mode", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Keep reminding until done", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = nagEnabled,
                        onCheckedChange = onNagEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = selectedColor
                        )
                    )
                }
            }
        }
        
        // 4. Look (Color)
        Card(
             colors = CardDefaults.cardColors(containerColor = itemBackgroundColor),
             shape = RoundedCornerShape(12.dp)
        ) {
             Column(modifier = Modifier.padding(12.dp)) {
                Text("Color", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom=8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Colors.noteColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color = color, shape = CircleShape)
                                .clickable { onColorChange(color) }
                                .border(
                                    width = if (selectedColor == color) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                        )
                    }
                }
             }
        }
        
        Spacer(Modifier.height(16.dp))

        // Save Button (Big & Bottom)
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = selectedColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Check, null)
            Spacer(Modifier.width(8.dp))
            Text("Save Note", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}


@Composable
private fun Day(
    day: CalendarDay,
    isSelected: Boolean = false,
    isHighlighted: Boolean = false,
    notes: List<CalendarNote> = emptyList(),
    onClick: (CalendarDay) -> Unit = {},
) {
    // Pulse animation for highlight (alpha)
    val pulseAlpha = remember { Animatable(0f) }
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            pulseAlpha.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseAlpha.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f) // This is important for square-sizing!
            .border(
                width = if (isHighlighted) 3.dp else if (isSelected) 1.dp else 0.dp,
                color = if (isHighlighted) Color(0xFFFFD700).copy(alpha = pulseAlpha.value) else if (isSelected) selectedItemColor else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(1.dp)
            .background(color = itemBackgroundColor)
            // Disable clicks on inDates/outDates
            .clickable(
                enabled = day.position == DayPosition.MonthDate,
                onClick = { onClick(day) },
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            
        ) {
            val grouped = notes.groupBy { it.color }
            grouped.forEach { (color, notesForColor) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((notesForColor.size * 4).dp) // "Squish" height logic
                        .background(color),
                )
            }
        }
        val textColor = when (day.position) {
            DayPosition.MonthDate -> Color.Unspecified
            DayPosition.InDate, DayPosition.OutDate -> inActiveTextColor
        }
        Text(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 3.dp, end = 4.dp),
            text = day.date.dayOfMonth.toString(),
            color = textColor,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun MonthHeader(
    modifier: Modifier = Modifier,
    daysOfWeek: List<DayOfWeek> = emptyList(),
) {
    Row(modifier.fillMaxWidth()) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color.White,
                text = dayOfWeek.displayText(uppercase = true),
                fontWeight = FontWeight.Light,
            )
        }
    }
}

@Composable
private fun LazyItemScope.NoteInformation(note: CalendarNote, onDelete: () -> Unit, onComplete: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillParentMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox for Completion
        Box(
            modifier = Modifier
                .background(color = itemBackgroundColor)
                .fillMaxHeight()
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = note.isCompleted,
                onCheckedChange = onComplete,
                colors = CheckboxDefaults.colors(
                    checkedColor = note.color,
                    uncheckedColor = Color.Gray,
                    checkmarkColor = Color.White
                )
            )
        }
        
        Box(
            modifier = Modifier
                .background(color = note.color)
                .fillParentMaxWidth(1 / 7f)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            // Using a simple time format derived from the helper, or simplifying it for notes
            // Just showing the hour for now or empty? The design had the date/time.
            // Let's use the date formatter but simple
             Text(
                text = "${note.time.hour}:${note.time.minute.toString().padStart(2, '0')}",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
            )
        }
        Box(
            modifier = Modifier
                .background(color = if (note.isCompleted) itemBackgroundColor.copy(alpha = 0.5f) else itemBackgroundColor)
                .weight(1f)
                .fillMaxHeight()
                .padding(8.dp),
                contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = note.text,
                color = if (note.isCompleted) Color.Gray else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = if (note.isCompleted) TextDecoration.LineThrough else null
            )
        }
        // Delete Button
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
                tint = Color.Gray
             )
        }
    }
    HorizontalDivider(thickness = 2.dp, color = pageBackgroundColor)
}

@Composable
private fun LazyItemScope.QueueNoteInformation(note: QueueNote, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillParentMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
         // Queue Indicator
         Box(
             modifier = Modifier
                 .background(color = Color.DarkGray)
                 .fillParentMaxWidth(1 / 7f)
                 .aspectRatio(1f),
             contentAlignment = Alignment.Center,
         ) {
              Text(
                  text = "Q",
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
              )
         }
         
         Box(
             modifier = Modifier
                 .background(color = itemBackgroundColor)
                 .weight(1f)
                 .fillMaxHeight()
                 .padding(8.dp),
             contentAlignment = Alignment.CenterStart
         ) {
             Text(
                 text = note.text,
                 color = Color.White,
                 fontSize = 16.sp,
                 fontWeight = FontWeight.Normal,
             )
         }
         
         // Delete Button
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
                 tint = Color.Gray
              )
         }
    }
    HorizontalDivider(thickness = 2.dp, color = pageBackgroundColor)
}

/*
@Preview
@Composable
private fun CalendarAppPreview() {
    // CalendarApp()
}
*/



// Helper extensions restored locally to fix visibility issues
// Using 48.dp as a safe buffer for status bars as requested
private fun Modifier.applyScaffoldHorizontalPaddingsLocal(): Modifier = this.padding(horizontal = 4.dp)
private fun Modifier.applyScaffoldTopPaddingLocal(): Modifier = this.padding(top = 48.dp)
private fun Modifier.applyScaffoldBottomPaddingLocal(): Modifier = this.padding(bottom = 0.dp)

private fun YearMonth.displayText(): String {
    return "${this.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${this.year}"
}

private fun kotlinx.datetime.DayOfWeek.displayText(uppercase: Boolean = false): String {
    val name = this.name.lowercase().replaceFirstChar { it.uppercase() }
    return if (uppercase) name.take(3).uppercase() else name.take(3)
}

// Helper for logging
private fun getFormattedTime(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}"
}
