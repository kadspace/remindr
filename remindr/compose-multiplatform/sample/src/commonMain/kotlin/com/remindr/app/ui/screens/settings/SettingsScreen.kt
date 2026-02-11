package com.remindr.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remindr.app.ui.theme.Colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onTestNotification: () -> Unit,
    onRichTestNotification: () -> Unit,
    logs: String,
    eventTypeLabels: List<String>,
    onEventTypeLabelsChange: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var showPassword by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Colors.example5PageBgColor,
        contentColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Colors.example5ToolbarColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // API Key
            Text(
                text = "AI CONFIGURATION",
                style = MaterialTheme.typography.labelSmall,
                color = Colors.example5TextGreyLight,
                modifier = Modifier.padding(start = 4.dp),
            )

            Surface(
                color = Colors.example5ItemViewBgColor,
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = onApiKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Groq API Key") },
                        placeholder = { Text("gsk_...") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Key")
                        },
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(
                                    if (showPassword) "HIDE" else "SHOW",
                                    fontSize = 11.sp,
                                    color = Colors.example5TextGrey,
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Colors.example5ToolbarColor,
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Colors.example5TextGrey,
                            cursorColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Get a free key at console.groq.com/keys",
                        style = MaterialTheme.typography.labelMedium,
                        color = Colors.example1Selection,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { uriHandler.openUri("https://console.groq.com/keys") }
                            .padding(vertical = 4.dp),
                    )
                }
            }

            // Event Types
            Text(
                text = "EVENT TYPES",
                style = MaterialTheme.typography.labelSmall,
                color = Colors.example5TextGreyLight,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )

            Surface(
                color = Colors.example5ItemViewBgColor,
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Colors.noteColors.forEachIndexed { index, color ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(color),
                            )
                            Spacer(Modifier.width(12.dp))

                            OutlinedTextField(
                                value = eventTypeLabels.getOrNull(index) ?: "",
                                onValueChange = { newLabel ->
                                    val updated = eventTypeLabels.toMutableList()
                                    if (index < updated.size) {
                                        updated[index] = newLabel
                                    } else {
                                        while (updated.size < index) updated.add("")
                                        updated.add(newLabel)
                                    }
                                    onEventTypeLabelsChange(updated)
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                singleLine = true,
                                placeholder = { Text("Type ${index + 1}", color = Colors.example5TextGreyLight) },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = color,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = color,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.1f),
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            // Notifications
            Text(
                text = "NOTIFICATIONS",
                style = MaterialTheme.typography.labelSmall,
                color = Colors.example5TextGreyLight,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )

            Surface(
                color = Colors.example5ItemViewBgColor,
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onTestNotification,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Test", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onRichTestNotification,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Rich Test", fontSize = 13.sp)
                    }
                }
            }

            // About
            Text(
                text = "ABOUT",
                style = MaterialTheme.typography.labelSmall,
                color = Colors.example5TextGreyLight,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )

            Surface(
                color = Colors.example5ItemViewBgColor,
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Version", color = Colors.example5TextGrey, fontSize = 14.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("v2.0.0", color = Color.White, fontSize = 14.sp)
                            Text(
                                "ALPHA",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Colors.accent, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Platform", color = Colors.example5TextGrey, fontSize = 14.sp)
                        Text("Kotlin Multiplatform", color = Color.White, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
