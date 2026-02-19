package com.remindr.app.ui.screens.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remindr.app.data.model.QuickNote
import com.remindr.app.data.model.QuickNoteState
import com.remindr.app.ui.theme.Colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    notes: List<QuickNote>,
    onEditNote: (QuickNote) -> Unit,
    onArchiveNote: (Long) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onRestoreNote: (Long) -> Unit,
) {
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var showDeleted by rememberSaveable { mutableStateOf(false) }
    var hiddenRows by remember { mutableStateOf(setOf<Pair<Long, QuickNoteState>>()) }
    val coroutineScope = rememberCoroutineScope()

    val currentRows = remember(notes) { notes.map { it.id to it.state }.toSet() }
    LaunchedEffect(currentRows) {
        hiddenRows = hiddenRows.filterTo(mutableSetOf()) { it in currentRows }
    }

    fun hideOptimistically(note: QuickNote) {
        val key = note.id to note.state
        hiddenRows = hiddenRows + key
        coroutineScope.launch {
            delay(1500)
            hiddenRows = hiddenRows - key
        }
    }

    val activeNotes = remember(notes, hiddenRows) {
        notes.filter { it.state == QuickNoteState.ACTIVE && (it.id to it.state) !in hiddenRows }
    }
    val archivedNotes = remember(notes, hiddenRows) {
        notes.filter { it.state == QuickNoteState.ARCHIVED && (it.id to it.state) !in hiddenRows }
    }
    val deletedNotes = remember(notes, hiddenRows) {
        notes.filter { it.state == QuickNoteState.DELETED && (it.id to it.state) !in hiddenRows }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp),
    ) {
        if (activeNotes.isEmpty()) {
            item {
                Text(
                    text = "No active notes.",
                    color = Colors.example5TextGreyLight,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                )
            }
        } else {
            items(activeNotes, key = { it.id }) { note ->
                SwipeableNoteRow(
                    note = note,
                    primaryActionLabel = "Archive",
                    secondaryActionLabel = "Delete",
                    onPrimaryAction = {
                        hideOptimistically(note)
                        onArchiveNote(note.id)
                    },
                    onSecondaryAction = {
                        hideOptimistically(note)
                        onDeleteNote(note.id)
                    },
                    onOpen = { onEditNote(note) },
                )
            }
        }

        item {
            NotesSectionToggle(
                label = "Archived",
                count = archivedNotes.size,
                expanded = showArchived,
                onToggle = { showArchived = !showArchived },
            )
        }

        if (showArchived) {
            if (archivedNotes.isEmpty()) {
                item {
                    Text(
                        text = "No archived notes.",
                        color = Colors.example5TextGreyLight,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 6.dp, top = 2.dp),
                    )
                }
            } else {
                items(archivedNotes, key = { it.id }) { note ->
                    SwipeableNoteRow(
                        note = note,
                        primaryActionLabel = "Restore",
                        secondaryActionLabel = "Delete",
                        onPrimaryAction = {
                            hideOptimistically(note)
                            onRestoreNote(note.id)
                        },
                        onSecondaryAction = {
                            hideOptimistically(note)
                            onDeleteNote(note.id)
                        },
                        onOpen = { onEditNote(note) },
                    )
                }
            }
        }

        item {
            NotesSectionToggle(
                label = "Deleted",
                count = deletedNotes.size,
                expanded = showDeleted,
                onToggle = { showDeleted = !showDeleted },
            )
        }

        if (showDeleted) {
            if (deletedNotes.isEmpty()) {
                item {
                    Text(
                        text = "No deleted notes.",
                        color = Colors.example5TextGreyLight,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 6.dp, top = 2.dp),
                    )
                }
            } else {
                items(deletedNotes, key = { it.id }) { note ->
                    SwipeableNoteRow(
                        note = note,
                        primaryActionLabel = "Restore",
                        secondaryActionLabel = null,
                        onPrimaryAction = {
                            hideOptimistically(note)
                            onRestoreNote(note.id)
                        },
                        onSecondaryAction = null,
                        onOpen = { onEditNote(note) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNoteRow(
    note: QuickNote,
    primaryActionLabel: String,
    secondaryActionLabel: String?,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: (() -> Unit)?,
    onOpen: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { total -> total * 0.45f },
    )
    var actionHandled by remember(note.id, note.state) { mutableStateOf(false) }

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
                label = "noteSwipeLabelAlpha",
            )
            val labelScale by animateFloatAsState(
                targetValue = if (isArmed) 1.07f else 0.94f,
                animationSpec = tween(durationMillis = 140),
                label = "noteSwipeLabelScale",
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
        NoteCard(
            note = note,
            onOpen = onOpen,
        )
    }
}

@Composable
private fun NoteCard(
    note: QuickNote,
    onOpen: () -> Unit,
) {
    var expanded by rememberSaveable(note.id, note.state) { mutableStateOf(false) }
    val collapsedTitle = note.content
        .lineSequence()
        .firstOrNull()
        .orEmpty()
        .ifBlank { "(empty note)" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Colors.example5ItemViewBgColor.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = collapsedTitle,
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse note" else "Expand note",
                    tint = Colors.example5TextGreyLight,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.01f))
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = note.content.ifBlank { "(empty note)" },
                        color = Color.White.copy(alpha = 0.93f),
                        fontSize = 13.sp,
                        modifier = Modifier.clickable(onClick = onOpen),
                    )
                    Text(
                        text = note.summaryLine,
                        color = Colors.example5TextGreyLight,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesSectionToggle(
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
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
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
