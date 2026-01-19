package com.kizitonwose.calendar.compose.multiplatform.sample

import kotlinx.coroutines.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var showPassword by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Colors.example5PageBgColor,
        contentColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    NavigationIcon(onBackClick = onBack)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // API Key Section
            SettingsCard(title = "AI Configuration") {
                Text(
                    "Enter your Groq API Key to enable Magic Add features.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Colors.example5TextGrey
                )
                
                Spacer(modifier = Modifier.height(16.dp))

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
                            Text(if (showPassword) "HIDE" else "SHOW", fontSize = 12.sp, color = Colors.example5TextGrey)
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
                        focusedContainerColor = Colors.example5ItemViewBgColor,
                        unfocusedContainerColor = Colors.example5ItemViewBgColor
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { uriHandler.openUri("https://console.groq.com/keys") }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Colors.example1Selection,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Get a free API key at console.groq.com",
                        style = MaterialTheme.typography.labelLarge,
                        color = Colors.example1Selection
                    )
                }
            }

            // Debug Section
            SettingsCard(title = "Debug") {
                Button(
                    onClick = {
                         onTestNotification()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Colors.example1Selection,
                        contentColor = Color.White
                    )
                ) {
                    Text("Trigger Test Notification")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                         onRichTestNotification()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Colors.accent,
                        contentColor = Color.White
                    )
                ) {
                    Text("Trigger Rich Notification")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = logs,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
            
            // Event Types Section (Editable)
            SettingsCard(title = "Event Types") {
                Text(
                    "Customize your event category names",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Colors.example5TextGrey
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Colors.noteColors.forEachIndexed { index, color ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            OutlinedTextField(
                                value = eventTypeLabels.getOrNull(index) ?: "",
                                onValueChange = { newLabel ->
                                    val updated = eventTypeLabels.toMutableList()
                                    if (index < updated.size) {
                                        updated[index] = newLabel
                                    } else {
                                        // Pad with empty strings if needed
                                        while (updated.size < index) {
                                            updated.add("")
                                        }
                                        updated.add(newLabel)
                                    }
                                    onEventTypeLabelsChange(updated)
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                placeholder = { Text("Type ${index + 1}", color = Colors.example5TextGreyLight) },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = color,
                                    unfocusedBorderColor = Colors.example5ToolbarColor,
                                    cursorColor = color,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
             // About Section
            SettingsCard(title = "About") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Version", color = Colors.example5TextGrey)
                    Column(horizontalAlignment = Alignment.End) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("v1.7.4", color = Color.White)
                            Text("ALPHA",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Colors.accent, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text("Built: Jan 18, 2025", color = Colors.example5TextGrey, fontSize = 10.sp)
                    }
                }
                HorizontalDivider(color = Colors.example5ToolbarColor, modifier = Modifier.padding(vertical = 12.dp))
                 Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Build", color = Colors.example5TextGrey)
                    Text("Kotlin Multiplatform", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Colors.example5TextGreyLight,
            modifier = Modifier.padding(start = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Colors.example5ItemViewBgColor, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            content()
        }
    }
}
