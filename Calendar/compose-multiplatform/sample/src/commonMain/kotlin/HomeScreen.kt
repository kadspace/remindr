package com.kizitonwose.calendar.compose.multiplatform.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onProcess: (String, Boolean) -> Unit // text, isReminder
) {
    var text by remember { mutableStateOf("") }
    var isReminder by remember { mutableStateOf(true) } // Default to Reminder (true)

    Column(
        modifier = modifier
            .fillMaxWidth() // Dialog width
            .background(Color(0xFF0E0E0E), shape = RoundedCornerShape(16.dp)) // Dialog Bg with corners
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "New Item",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("What's on your mind?", color = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1B1B1B), // Dark Item Bg
                unfocusedContainerColor = Color(0xFF1B1B1B),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Type Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilterChip(
                selected = !isReminder,
                onClick = { isReminder = false },
                label = { Text("Note", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF333333), // Dark Gray
                    labelColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            )

            FilterChip(
                selected = isReminder,
                onClick = { isReminder = true },
                label = { Text("Reminder", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF333333), // Dark Gray
                    labelColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onProcess(text, isReminder) },
            enabled = text.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                 containerColor = if (isReminder) Color(0xFFFF4081) else Color(0xFF3F51B5)
            )
        ) {
            Text("Save", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
