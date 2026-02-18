package com.remindr.app.ui.screens.notes

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remindr.app.data.model.QuickNote
import com.remindr.app.ui.theme.Colors

@Composable
fun NotesScreen(
    notes: List<QuickNote>,
    onUpdateNote: (Long, String) -> Unit,
    onArchiveNote: (Long) -> Unit,
    onPromoteToReminder: (QuickNote) -> Unit,
) {
    var editingNote by remember { mutableStateOf<QuickNote?>(null) }
    var editingText by remember { mutableStateOf("") }

    if (editingNote != null) {
        AlertDialog(
            onDismissRequest = { editingNote = null },
            title = { Text("Edit note") },
            text = {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val note = editingNote
                        if (note != null) {
                            onUpdateNote(note.id, editingText)
                        }
                        editingNote = null
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNote = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
    ) {
        item {
            Text(
                text = "Notes Inbox",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Quick dump only. Promote later when needed.",
                color = Colors.example5TextGreyLight,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        if (notes.isEmpty()) {
            item {
                Text(
                    text = "No notes yet.",
                    color = Colors.example5TextGreyLight,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            items(notes, key = { it.id }) { note ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingNote = note
                            editingText = note.content
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Colors.example5ItemViewBgColor.copy(alpha = 0.92f)),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = note.content,
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = note.summaryLine,
                            color = Colors.example5TextGreyLight,
                            fontSize = 11.sp,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(onClick = {
                                    editingNote = note
                                    editingText = note.content
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit note",
                                        tint = Colors.example5TextGreyLight,
                                    )
                                }
                                IconButton(onClick = { onArchiveNote(note.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Archive,
                                        contentDescription = "Archive note",
                                        tint = Colors.example5TextGreyLight,
                                    )
                                }
                            }

                            TextButton(onClick = { onPromoteToReminder(note) }) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Promote to reminder",
                                    tint = Colors.example5TextGreyLight,
                                )
                                Text(
                                    text = "Promote",
                                    color = Colors.example5TextGreyLight,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

