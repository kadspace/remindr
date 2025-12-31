package com.kizitonwose.calendar.compose.multiplatform.sample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.kizitonwose.calendar.compose.multiplatform.sample.rememberSpeechRecognizer
import com.kizitonwose.calendar.sample.db.RemindrDatabase
import com.kizitonwose.calendar.compose.multiplatform.sample.DatabaseDriverFactory
import com.kizitonwose.calendar.compose.multiplatform.sample.NoteDbHelper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.kizitonwose.calendar.compose.multiplatform.sample.SimpleCalendarTitle
import com.kizitonwose.calendar.compose.multiplatform.sample.rememberFirstCompletelyVisibleMonth
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.plusMonths
import com.kizitonwose.calendar.core.now
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filter
import kotlinx.datetime.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview





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

@Composable
fun CalendarApp(driverFactory: DatabaseDriverFactory) {
    // Database Init
    val database = remember { RemindrDatabase(driverFactory.createDriver()) }
    val dbHelper = remember { NoteDbHelper(database) }
    
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(500) }
    val endMonth = remember { currentMonth.plusMonths(500) }
    var selection by remember { mutableStateOf<CalendarDay?>(null) }
    val daysOfWeek = remember { daysOfWeek() }

    // Load notes from DB
    val notes = dbHelper.getAllNotes().collectAsState(initial = emptyList()).value
    
    val notesInSelectedDate = remember(notes, selection) {
        derivedStateOf {
            val date = selection?.date
            if (date == null) emptyList() else notes.filter { it.time.date == date }
        }
    }

    // State for new note input
    var isSheetOpen by remember { mutableStateOf(false) }
    
    // Hoisted State for Split View
    var text by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(noteColors.first()) }
    var selectedTime by remember { mutableStateOf(LocalTime(8, 0)) }
    
    var apiKey by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        dbHelper.getApiKey()?.let { apiKey = it }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            initialKey = apiKey,
            onDismiss = { showSettingsDialog = false },
            onSave = { newKey ->
                apiKey = newKey
                dbHelper.saveApiKey(newKey)
                showSettingsDialog = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBackgroundColor)
                .applyScaffoldHorizontalPaddingsLocal()
                .applyScaffoldBottomPaddingLocal(),
        ) {
            val state = rememberCalendarState(
                startMonth = startMonth,
                endMonth = endMonth,
                firstVisibleMonth = currentMonth,
                firstDayOfWeek = daysOfWeek.first(),
                outDateStyle = OutDateStyle.EndOfGrid,
            )
            val coroutineScope = rememberCoroutineScope()
            val visibleMonth = rememberFirstCompletelyVisibleMonth(state)
            LaunchedEffect(visibleMonth) {
                // Clear selection if we scroll to a new month.
                selection = null
            }

        // Draw light content on dark background.
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
                onSettingsClick = { showSettingsDialog = true }
            )
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
            HorizontalDivider(color = pageBackgroundColor)
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(items = notesInSelectedDate.value) { note ->
                    NoteInformation(note, onDelete = { dbHelper.deleteById(note.id) })
                }
            }



            val aiService = remember { AIService() }
            var showMagicDialog by remember { mutableStateOf(false) }
            var isThinking by remember { mutableStateOf(false) }
            var magicError by remember { mutableStateOf<String?>(null) }
            
            if (showMagicDialog) {
                MagicDialog(
                    onDismiss = { showMagicDialog = false },
                    initialKey = apiKey,
                    error = magicError,
                    isLoading = isThinking,
                    onConfirm = { magicText, key ->
                        isThinking = true
                        magicError = null
                        coroutineScope.launch {
                            try {
                                val result = aiService.parseNote(magicText, key)
                                println("Magic Add Result: $result") // LOGGING
                                
                                // If successful
                                if (result != null) {
                                    val colorIdx = result.colorIndex.coerceIn(0, 7)
                                    
                                    // Calculate target day (Default to start of current month as fallback for now)
                                    val nowYM = YearMonth.now() 
                                    val topDay = kotlinx.datetime.LocalDate(nowYM.year, nowYM.month, 1)
                                    val targetDate = topDay.plus(DatePeriod(days = result.dayOffset))
                                    
                                    // Construct Note
                                    val time = targetDate.atTime(result.hour, result.minute)
                                    val note = CalendarNote(
                                        time = time,
                                        text = result.text,
                                        color = noteColors[colorIdx]
                                    )
                                    
                                    // Save to DB
                                    dbHelper.insert(note)

                                    // Move selection to that day so user sees the new dot
                                    selection = CalendarDay(targetDate, DayPosition.MonthDate)

                                    // Close Dialog, Do NOT open sheet
                                    showMagicDialog = false
                                    magicError = null // Clear any error
                                }
                            } catch (e: Exception) {
                                magicError = "Error: ${e.message ?: e.toString()}"
                                e.printStackTrace()
                            } finally {
                                isThinking = false
                            }
                        }
                    }
                )
            }

            // Bottom Input Field (Trigger) - Always Visible when sheet is CLOSED
            if (!isSheetOpen) {
               Row(
                   modifier = Modifier
                       .fillMaxWidth()
                       .background(pageBackgroundColor)
                       .padding(16.dp)
                       .navigationBarsPadding()
                       .imePadding(),
                   horizontalArrangement = Arrangement.spacedBy(8.dp),
                   verticalAlignment = Alignment.CenterVertically
               ) {
                   // Magic Button (Outside)
                   IconButton(
                        onClick = { showMagicDialog = true },
                        modifier = Modifier.background(itemBackgroundColor, CircleShape)
                   ) {
                        if (isThinking) {
                              androidx.compose.material3.CircularProgressIndicator(
                                  modifier = Modifier.size(24.dp),
                                  color = Color.White,
                                  strokeWidth = 2.dp
                              )
                        } else {
                             Icon(androidx.compose.material.icons.Icons.Filled.AutoFixHigh, contentDescription = "Magic AI", tint = Color.Yellow)
                        }
                   }

                   OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Type a note...", color = Color.White) },
                        readOnly = true, // It's just a trigger
                        enabled = true,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isSheetOpen = true }, 
                        interactionSource = remember { MutableInteractionSource() }
                            .also { interactionSource ->
                                LaunchedEffect(interactionSource) {
                                    interactionSource.interactions.collect {
                                        if (it is PressInteraction.Release) {
                                            isSheetOpen = true
                                        }
                                    }
                                }
                            },
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
                }
            }


        }
    }

        // Overlay Sheet
        AnimatedVisibility(
            visible = isSheetOpen && selection != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.fillMaxSize().zIndex(1f) // Ensure on top
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { isSheetOpen = false } // Dismiss on scrim click
            ) {
                 // Stop propagation of clicks to the scrim
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter) // Smart Bottom Panel
                        .fillMaxWidth()
                        .background(
                             color = pageBackgroundColor,
                             shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp) // Top corners rounded
                        )
                        .clickable(enabled = false) {} // Prevent clicks passing through content
                        .padding(16.dp)
                        .navigationBarsPadding() 
                        .imePadding(), // Push up with keyboard
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                   // Spacer(Modifier.height(24.dp)) // No extra top padding needed for bottom sheet

                   AddNoteContent(
                       text = text,
                       onTextChange = { text = it },
                       selectedColor = selectedColor,
                       selectedTime = selectedTime,
                       onColorChange = { selectedColor = it },
                       onTimeChange = { selectedTime = it },
                        onAdd = {
                            val date = selection?.date ?: kotlinx.datetime.LocalDate(currentMonth.year, currentMonth.month, 1)
                            dbHelper.insert(CalendarNote(time = date.atTime(selectedTime), text = text, color = selectedColor))
                            
                            text = "" // Clear text on add
                            isSheetOpen = false
                       }
                   )
                }
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
    notes: List<CalendarNote> = emptyList(),
    onClick: (CalendarDay) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f) // This is important for square-sizing!
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) selectedItemColor else Color.Transparent,
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

/*
@Preview
@Composable
private fun CalendarAppPreview() {
    // CalendarApp()
}
*/

@Composable
fun MagicDialog(
    onDismiss: () -> Unit,
    initialKey: String,
    error: String? = null,
    isLoading: Boolean = false,
    onConfirm: (String, String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf(initialKey) } // Init from persisted key
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Magic Add", color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Describe your event:", color = Color.Black)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("e.g. Lunch with John at 1pm") },
                    enabled = !isLoading
                )
                Text("Groq API Key:", color = Color.Black, fontSize = 12.sp)
                 OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    placeholder = { Text("Paste API Key here") },
                    singleLine = true,
                    enabled = !isLoading
                )
                if (error != null) {
                    Text(error, color = Color.Red, fontSize = 12.sp)
                } else {
                    Text("Get a free key from console.groq.com", fontSize = 10.sp, color = Color.Gray)
                }
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally),
                        color = Color.Blue
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text, apiKey) },
                enabled = text.isNotBlank() && apiKey.isNotBlank() && !isLoading
            ) {
                Text(if (isLoading) "Thinking..." else "Magify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        },
        containerColor = Color.White // Force light background for dialog
    )
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



