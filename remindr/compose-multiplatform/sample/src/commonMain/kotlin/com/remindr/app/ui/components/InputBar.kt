package com.remindr.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remindr.app.ui.theme.Colors

private val pageBackgroundColor: Color = Colors.example5PageBgColor

@Composable
fun InputBar(
    modifier: Modifier = Modifier,
    placeholder: String = "New Reminder...",
    isSaving: Boolean = false,
    onSend: (String) -> Unit,
) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(pageBackgroundColor)
            .animateContentSize(),
    ) {
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(placeholder, color = Color.Gray, fontSize = 14.sp) },
                modifier = Modifier.weight(1f).heightIn(min = 40.dp, max = 100.dp),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                trailingIcon = {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                        )
                    } else if (inputText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val text = inputText
                                inputText = ""
                                onSend(text)
                            },
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                "Send",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
            )
        }
    }
}
