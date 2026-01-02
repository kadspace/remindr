package com.kizitonwose.calendar.compose.multiplatform.sample

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

private val noteColors = listOf(
    Color(0xFF1565C0),
    Color(0xFFC62828),
    Color(0xFF5D4037),
    Color(0xFF455A64),
    Color(0xFF00796B),
    Color(0xFF0097A7),
    Color(0xFFC2185B),
    Color(0xFFEF6C00),
)

enum class Screen {
    Calendar, Settings
}

enum class Tab { Calendar, Notes }
enum class CalendarViewMode { Year, Month }

@Composable
fun CalendarApp(driverFactory: DatabaseDriverFactory, requestMagicAdd: Boolean = false) {
    var screen by remember { mutableStateOf(Screen.Calendar) }
    var currentTab by remember { mutableStateOf(Tab.Calendar) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.Month) }
    
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
    var text by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(noteColors.first()) }
    var selectedTime by remember { mutableStateOf(LocalTime(8, 0)) }

    when (screen) {
        Screen.Settings -> {
            SettingsScreen(
                apiKey = apiKey,
                onApiKeyChange = { newKey ->
                    apiKey = newKey
                    dbHelper.saveApiKey(newKey)
                },
                onBack = { screen = Screen.Calendar }
            )
        }
        Screen.Calendar -> {
            // Hoisted State for Overlay & Calendar
            val snackbarHostState = remember { SnackbarHostState() }
            val aiService = remember { AIService() }
            var isThinking by remember { mutableStateOf(false) }
            var magicError by remember { mutableStateOf<String?>(null) }
            var debugLogs by remember { mutableStateOf("") }

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
                                onSettingsClick = { screen = Screen.Settings }
                            )
                        }
                    },
                    bottomBar = {
                        if (sheetMode == 0 || sheetMode == 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(pageBackgroundColor)
                                    .navigationBarsPadding()
                                    .imePadding() // Key for keyboard
                            ) {
                                MagicInputContent(
                                   error = magicError,
                                   isLoading = isThinking,
                                   onSwitchToManual = { sheetMode = 2 },
                                   onConfirm = { magicText ->
                                       if (currentTab == Tab.Notes) {
                                           coroutineScope.launch {
                                               dbHelper.insertQueue(magicText)
                                               // Optional: Scroll to top of queue?
                                           }
                                       } else {
                                           isThinking = true
                                           magicError = null
                                           coroutineScope.launch {
                                               try {
                                                   val today = getToday()
                                                   val result = aiService.parseNote(magicText, apiKey, today)
                                                   
                                                   if (result != null) {
                                                       val colorIdx = result.colorIndex.coerceIn(0, 7)
                                                       val targetDate = if (result.year != null && result.month != null && result.day != null) {
                                                           LocalDate(result.year, result.month, result.day)
                                                       } else {
                                                           today
                                                       }
                                                       val time = targetDate.atTime(result.hour, result.minute)
                                                       
                                                       val note = CalendarNote(
                                                           time = time,
                                                           text = result.text,
                                                           color = noteColors[colorIdx]
                                                       )
                                                       dbHelper.insert(note)
                                                       val insertedId = dbHelper.getLastInsertedNoteId()
                                                       
                                                       selection = CalendarDay(targetDate, DayPosition.MonthDate)
                                                       recentlyAddedDate = targetDate
                                                       
                                                       val targetMonth = YearMonth(targetDate.year, targetDate.month)
                                                       if (targetMonth != state.firstVisibleMonth.yearMonth) {
                                                           state.animateScrollToMonth(targetMonth)
                                                       }
                                                       
                                                       sheetMode = 0 
                                                       
                                                       val snackResult = snackbarHostState.showSnackbar(
                                                           message = "Saved: ${result.text} ($targetDate)",
                                                           actionLabel = "UNDO",
                                                           duration = SnackbarDuration.Short
                                                       )
                                                       
                                                       if (snackResult == SnackbarResult.ActionPerformed && insertedId != null) {
                                                           dbHelper.deleteById(insertedId)
                                                            snackbarHostState.showSnackbar("Note deleted")
                                                       }
                                                   } else {
                                                       magicError = "Could not understand note."
                                                   }
                                               } catch (e: Exception) {
                                                   e.printStackTrace()
                                                   magicError = "Error: ${e.message ?: e.toString()}"
                                               } finally {
                                                   isThinking = false
                                               }
                                           }
                                       }
                                   }
                               )
                            }
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .applyScaffoldHorizontalPaddingsLocal()
                    ) {
                        
                        CompositionLocalProvider(LocalContentColor provides Color.White) {
                            // View Mode Switcher
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val modes = CalendarViewMode.values()
                                modes.forEach { mode ->
                                    val isSelected = viewMode == mode
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) selectedItemColor else Color.Transparent)
                                            .clickable { viewMode = mode }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = mode.name,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else inActiveTextColor
                                        )
                                    }
                                }
                            }

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

                            
                            // Tab Switcher
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Button(
                                    onClick = { currentTab = Tab.Calendar },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (currentTab == Tab.Calendar) selectedItemColor else Color.Transparent,
                                        contentColor = if (currentTab == Tab.Calendar) Color.White else inActiveTextColor
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                                ) {
                                    Text("Calendar", fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    onClick = { currentTab = Tab.Notes },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (currentTab == Tab.Notes) selectedItemColor else Color.Transparent,
                                        contentColor = if (currentTab == Tab.Notes) Color.White else inActiveTextColor
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                                ) {
                                    Text("Notes", fontSize = 12.sp)
                                }
                            }

                            HorizontalDivider(color = pageBackgroundColor)
                            
                            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                if (currentTab == Tab.Calendar) {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(items = notesInSelectedDate.value) { note ->
                                            NoteInformation(note, onDelete = { dbHelper.deleteById(note.id) })
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
                }
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
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                     AddNoteContent(
                                         text = text,
                                         onTextChange = { text = it },
                                         selectedColor = selectedColor,
                                         selectedTime = selectedTime,
                                         onColorChange = { selectedColor = it },
                                         onTimeChange = { selectedTime = it },
                                         onAdd = {
                                             val date = selection?.date ?: getToday()
                                             val note = CalendarNote(time = date.atTime(selectedTime), text = text, color = selectedColor)
                                             dbHelper.insert(note)
                                             val insertedId = dbHelper.getLastInsertedNoteId()
                                             
                                             text = ""
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
                                                 val snackResult = snackbarHostState.showSnackbar(
                                                     message = "Saved note for $date",
                                                     actionLabel = "UNDO",
                                                     duration = SnackbarDuration.Short
                                                 )
                                                 if (snackResult == SnackbarResult.ActionPerformed && insertedId != null) {
                                                     dbHelper.deleteById(insertedId)
                                                     snackbarHostState.showSnackbar("Note deleted")
                                                 }
                                             }
                                         }
                                     )
                                }
                            }
                        }
            

            // Removed duplicate SnackbarHost from here as it is now in Scaffold
        }
    }
}
}

@Composable
private fun AddNoteContent(
    text: String,
    onTextChange: (String) -> Unit,
    selectedColor: Color,
    selectedTime: LocalTime,
    onColorChange: (Color) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    onAdd: () -> Unit,
) {
    val timePresets = listOf(
        LocalTime(8, 0),
        LocalTime(12, 0),
        LocalTime(15, 0)
    )
    val focusRequester = remember { FocusRequester() }

    /* ... Speech Recognizer ... */
    val launchSpeech = rememberSpeechRecognizer { spokenText ->
        val separator = if (text.isBlank()) "" else " "
        onTextChange(text + separator + spokenText)
    }
 

    // Auto-focus logic still nice
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Stack Order: Option -> Option -> Input Row (Bottom Anchor) with Add Button

        // Time Selection - Presets
        Text("Select Time", fontWeight = FontWeight.Medium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            timePresets.forEach { time ->
                val isSelected = selectedTime == time
                val label = when(time.hour) {
                    8 -> "8 AM"
                    12 -> "12 PM"
                    15 -> "3 PM"
                    else -> "${time.hour}:${time.minute}"
                }
                Button(
                    onClick = { onTimeChange(time) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) selectedColor else itemBackgroundColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(label, fontSize = 12.sp, maxLines = 1)
                }
            }
        }

        // Color Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            noteColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
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
        
        // Input Row (Anchor)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
             // Voice Input Button
             IconButton(
                 onClick = { launchSpeech() },
                 modifier = Modifier.background(itemBackgroundColor, CircleShape)
             ) {
                 Icon(androidx.compose.material.icons.Icons.Filled.Mic, contentDescription = "Voice Input", tint = Color.White)
             }

             OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text("Note", color = Color.White) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { 
                        if (text.isNotBlank()) onAdd() 
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = itemBackgroundColor,
                    unfocusedContainerColor = itemBackgroundColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedIndicatorColor = selectedColor,
                    unfocusedIndicatorColor = Color.Gray
                )
            )
            
            Button(
                onClick = { if (text.isNotBlank()) onAdd() },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = selectedColor),
                modifier = Modifier.height(IntrinsicSize.Max) // Match height if possible or standard
            ) {
                 Text("Add")
            }
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
private fun LazyItemScope.NoteInformation(note: CalendarNote, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillParentMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
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
                fontWeight = FontWeight.Bold,
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

@Composable
fun MagicInputContent(
    error: String?,
    isLoading: Boolean,
    onSwitchToManual: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    val launchSpeech = rememberSpeechRecognizer { spokenText ->
        val separator = if (text.isBlank()) "" else " "
        text = text + separator + spokenText
    }
    
    // Auto-focus on first load if desired (optional for persistent bar, maybe annoying if it pops keyboard immediately)
    // Removed auto-focus to avoid keyboard popping up on app launch.
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Colors.example5ItemViewBgColor) 
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (error != null) {
            Text(error, color = Color.Red, fontSize = 12.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
             // Manual Mode Toggle ("Writing" / Edit Icon)
             IconButton(onClick = onSwitchToManual) {
                 Icon(
                     imageVector = Icons.Default.Edit, // UPDATED ICON
                     contentDescription = "Manual Mode", 
                     tint = Color.Gray,
                     modifier = Modifier.size(24.dp)
                 )
             }
             
             OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("New Note...", fontSize = 14.sp, color = Color.Gray) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                minLines = 1,
                maxLines = 3,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedIndicatorColor = Color.Transparent, // Clean look
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            
            // Mic Icon (Always visible if text is empty, or prioritized)
            if (text.isBlank()) {
                IconButton(onClick = { launchSpeech() }) {
                     Icon(
                         imageVector = Icons.Default.Mic, 
                         contentDescription = "Voice Input", 
                         tint = Color.Gray
                     )
                }
            }

            // Send/Save Button (Visible when text is present)
            if (text.isNotBlank()) {
                IconButton(
                    onClick = { 
                        onConfirm(text) 
                        text = "" // Clear after send
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .background(Color.White, CircleShape)
                        .size(40.dp)
                ) {
                    if (isLoading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.Black
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check, 
                            contentDescription = "Save",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}


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
