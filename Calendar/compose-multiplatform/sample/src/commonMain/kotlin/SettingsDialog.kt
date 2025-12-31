package com.kizitonwose.calendar.compose.multiplatform.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsDialog(
    initialKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var apiKey by remember { mutableStateOf(initialKey) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings", color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gemini API Key:", color = Color.Black)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    placeholder = { Text("Paste API Key here") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                         focusedContainerColor = Color.White,
                         unfocusedContainerColor = Color.White,
                         focusedTextColor = Color.Black,
                         unfocusedTextColor = Color.Black,
                         cursorColor = Color.Black
                    )
                )
                Text("Get a free key from Google AI Studio", fontSize = 10.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(apiKey) },
                enabled = apiKey.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = Color.White
    )
}
