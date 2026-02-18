package com.remindr.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.remindr.app.ui.components.RemindrWordmark
import com.remindr.app.ui.theme.Colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    apiKey: String,
    versionLabel: String,
    onApiKeyChange: (String) -> Unit,
    onTestNotification: () -> Unit,
    onRichTestNotification: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    logs: String,
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var showPassword by remember { mutableStateOf(false) }
    val hasApiKey = apiKey.trim().isNotEmpty()

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

                    Text(
                        text = if (hasApiKey) {
                            val suffix = apiKey.takeLast(4)
                            "Saved key detected (${apiKey.length} chars, ends with $suffix)."
                        } else {
                            "No key saved."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Colors.example5TextGreyLight,
                        modifier = Modifier.padding(top = 6.dp),
                    )

                    if (hasApiKey) {
                        TextButton(
                            onClick = { onApiKeyChange("") },
                            modifier = Modifier.padding(top = 2.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                        ) {
                            Text("Clear saved key", color = Colors.example1Selection, fontSize = 12.sp)
                        }
                    }

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
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Enable exact alarms for reliable reminder delivery.",
                        color = Colors.example5TextGreyLight,
                        style = MaterialTheme.typography.bodySmall,
                    )

                    OutlinedButton(
                        onClick = onRequestExactAlarmPermission,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Enable Exact Alarms", fontSize = 13.sp)
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(8.dp))
                    RemindrWordmark(iconSize = 36.dp, fontSize = 20.sp)
                    Spacer(Modifier.height(20.dp))
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
                            Text(versionLabel, color = Color.White, fontSize = 14.sp)
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
