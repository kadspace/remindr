package com.kizitonwose.calendar.compose.multiplatform.sample

import androidx.compose.runtime.Composable

@Composable
expect fun rememberSpeechRecognizer(onResult: (String) -> Unit): () -> Unit
