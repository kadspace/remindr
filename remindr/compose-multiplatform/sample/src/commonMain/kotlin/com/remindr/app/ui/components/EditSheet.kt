package com.remindr.app.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remindr.app.data.model.ItemStatus
import com.remindr.app.data.model.ItemType
import com.remindr.app.data.model.Severity
import com.remindr.app.ui.theme.Colors

private val itemBackgroundColor: Color = Colors.example5ItemViewBgColor

@Composable
fun EditSheet(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    selectedColor: Color,
    onColorChange: (Color) -> Unit,
    severity: Severity,
    onSeverityChange: (Severity) -> Unit,
    itemType: ItemType,
    onItemTypeChange: (ItemType) -> Unit,
    itemStatus: ItemStatus,
    onItemStatusChange: (ItemStatus) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val isCritical = severity == Severity.HIGH

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(bottom = 16.dp),
    ) {
        // Title Input
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text("What's on your mind?", color = Color.White.copy(alpha = 0.5f)) },
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
                unfocusedIndicatorColor = Color.Gray,
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )

        // Type Selector (horizontal chips)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ItemType.entries.forEach { type ->
                FilterChip(
                    selected = itemType == type,
                    onClick = { onItemTypeChange(type) },
                    label = { Text(type.name, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = selectedColor.copy(alpha = 0.3f),
                        selectedLabelColor = Color.White,
                        containerColor = itemBackgroundColor,
                        labelColor = Color.Gray,
                    ),
                    border = if (itemType == type) FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = true,
                        borderColor = selectedColor,
                    ) else null,
                    modifier = Modifier.height(32.dp),
                )
            }
        }

        // Status Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(ItemStatus.PENDING, ItemStatus.IN_PROGRESS, ItemStatus.MONITORING, ItemStatus.COMPLETED).forEach { status ->
                FilterChip(
                    selected = itemStatus == status,
                    onClick = { onItemStatusChange(status) },
                    label = { Text(status.name.replace("_", " "), fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = selectedColor.copy(alpha = 0.3f),
                        selectedLabelColor = Color.White,
                        containerColor = itemBackgroundColor,
                        labelColor = Color.Gray,
                    ),
                    modifier = Modifier.height(32.dp),
                )
            }
        }

        // Amount field
        OutlinedTextField(
            value = amount,
            onValueChange = { newVal ->
                // Only allow numbers and decimal point
                if (newVal.isEmpty() || newVal.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                    onAmountChange(newVal)
                }
            },
            placeholder = { Text("Amount ($)", color = Color.White.copy(alpha = 0.3f)) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedIndicatorColor = selectedColor,
                unfocusedIndicatorColor = Color.Gray,
            ),
            singleLine = true,
            prefix = { if (amount.isNotEmpty()) Text("$ ", color = Color.White) },
        )

        // Severity Toggle: Badge vs Line
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSeverityChange(Severity.MEDIUM) },
                colors = CardDefaults.cardColors(
                    containerColor = if (!isCritical) selectedColor.copy(alpha = 0.3f) else itemBackgroundColor,
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (!isCritical) BorderStroke(2.dp, selectedColor) else null,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(selectedColor, CircleShape),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Badge", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSeverityChange(Severity.HIGH) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isCritical) selectedColor.copy(alpha = 0.3f) else itemBackgroundColor,
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (isCritical) BorderStroke(2.dp, selectedColor) else null,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(6.dp)
                            .background(selectedColor, RoundedCornerShape(3.dp)),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Critical", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Color Picker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Colors.noteColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color = color, shape = CircleShape)
                        .clickable { onColorChange(color) }
                        .border(
                            width = if (selectedColor == color) 3.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape,
                        ),
                )
            }
        }

        // Save Button
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = selectedColor),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
